package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.model.OtpRebuildState;
import ba.unsa.etf.pnwt.routingservice.repository.OtpRebuildStateRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class OtpRebuildService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpRebuildService.class);
    private static final Long SINGLETON_ID = 1L;

    private final OtpRebuildStateRepository repository;

    public OtpRebuildService(OtpRebuildStateRepository repository) {
        this.repository = repository;
    }

    public OtpRebuildState get() {
        return repository.findById(SINGLETON_ID).orElseGet(() -> {
            OtpRebuildState state = new OtpRebuildState();
            return repository.save(state);
        });
    }

    public void markRebuildNeeded() {
        OtpRebuildState state = get();
        if (state.isNeedsRebuild() && !state.isRebuildInProgress()) {
            state.setLastDataChangeAt(Instant.now());
            return;
        }
        if (state.isRebuildInProgress()) {
            return;
        }
        state.setNeedsRebuild(true);
        state.setLastDataChangeAt(Instant.now());
        state.setStatus(OtpRebuildState.RebuildStatus.IDLE);
        state.setErrorMessage(null);
        repository.save(state);
        LOGGER.info("OTP graph rebuild marked as needed");
    }

    public void markRebuildStarted(String triggeredBy) {
        OtpRebuildState state = get();
        state.setRebuildInProgress(true);
        state.setRebuildTriggeredBy(triggeredBy);
        state.setStatus(OtpRebuildState.RebuildStatus.BUILDING_GRAPH);
        state.setErrorMessage(null);
        repository.save(state);
        LOGGER.info("OTP graph rebuild started by {}", triggeredBy);
    }

    public void updateStatus(OtpRebuildState.RebuildStatus status) {
        OtpRebuildState state = get();
        state.setStatus(status);
        repository.save(state);
    }

    public void markRebuildCompleted() {
        OtpRebuildState state = get();
        state.setNeedsRebuild(false);
        state.setRebuildInProgress(false);
        state.setLastRebuildAt(Instant.now());
        state.setStatus(OtpRebuildState.RebuildStatus.COMPLETED);
        state.setErrorMessage(null);
        repository.save(state);
        LOGGER.info("OTP graph rebuild completed successfully");
    }

    public void markRebuildFailed(String errorMessage) {
        OtpRebuildState state = get();
        state.setRebuildInProgress(false);
        state.setStatus(OtpRebuildState.RebuildStatus.FAILED);
        state.setErrorMessage(errorMessage);
        repository.save(state);
        LOGGER.error("OTP graph rebuild failed: {}", errorMessage);
    }
}
