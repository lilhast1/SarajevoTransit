package com.sarajevotransit.moneyman.service;

import com.sarajevotransit.moneyman.exception.PaymentFailedException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Thin wrapper around the Stripe API for charging and refunding ticket purchases.
 * Cards are saved as Stripe PaymentMethod tokens (e.g. test tokens like {@code pm_card_visa});
 * a charge creates a confirmed PaymentIntent against that token.
 */
@Slf4j
@Service
public class StripePaymentService {

    private final String secretKey;
    private final String currency;

    public StripePaymentService(@Value("${stripe.secret-key:}") String secretKey,
                                @Value("${stripe.currency:eur}") String currency) {
        this.secretKey = secretKey;
        this.currency = currency;
    }

    @PostConstruct
    void init() {
        if (StringUtils.hasText(secretKey)) {
            Stripe.apiKey = secretKey;
            log.info("Stripe payment gateway initialized (currency: {})", currency);
        } else {
            log.warn("Stripe secret key not configured — ticket purchases will fail until STRIPE_SECRET_KEY is set");
        }
    }

    /**
     * Charges the given amount against a saved Stripe PaymentMethod token.
     *
     * @param amount             the price in major currency units (e.g. 1.80)
     * @param paymentMethodToken the Stripe PaymentMethod id (e.g. {@code pm_card_visa})
     * @return the id of the succeeded PaymentIntent (used as the external transaction id)
     * @throws PaymentFailedException if the gateway is unconfigured, declines, or errors
     */
    public String charge(BigDecimal amount, String paymentMethodToken) {
        if (!StringUtils.hasText(secretKey)) {
            throw new PaymentFailedException("Payment gateway is not configured");
        }
        if (!StringUtils.hasText(paymentMethodToken)) {
            throw new PaymentFailedException("Payment method has no gateway token");
        }

        long minorUnits = amount.movePointRight(2).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(minorUnits)
                .setCurrency(currency)
                .setPaymentMethod(paymentMethodToken)
                .setConfirm(true)
                // Disable redirect-based methods so no return_url is required for a server-side charge.
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build())
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            if (!"succeeded".equals(intent.getStatus())) {
                throw new PaymentFailedException("Payment was not completed (status: " + intent.getStatus() + ")");
            }
            log.info("Stripe charge succeeded: {} {} via {} — PaymentIntent {}",
                    amount, currency.toUpperCase(), paymentMethodToken, intent.getId());
            return intent.getId();
        } catch (StripeException e) {
            log.warn("Stripe charge failed for token {}: {}", paymentMethodToken, e.getMessage());
            throw new PaymentFailedException(
                    e.getStripeError() != null && e.getStripeError().getMessage() != null
                            ? e.getStripeError().getMessage()
                            : "Card was declined", e);
        }
    }

    /**
     * Best-effort refund used by the saga compensation path. Failures are logged but not propagated,
     * since the compensating transaction must still complete.
     */
    public void refund(String paymentIntentId) {
        if (!StringUtils.hasText(secretKey) || !StringUtils.hasText(paymentIntentId)) {
            return;
        }
        try {
            Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build());
            log.info("Stripe refund issued for PaymentIntent {}", paymentIntentId);
        } catch (StripeException e) {
            log.error("Stripe refund failed for PaymentIntent {}: {}", paymentIntentId, e.getMessage());
        }
    }
}
