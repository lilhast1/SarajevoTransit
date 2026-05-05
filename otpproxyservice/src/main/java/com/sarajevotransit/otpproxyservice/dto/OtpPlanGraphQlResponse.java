package com.sarajevotransit.otpproxyservice.dto;

public class OtpPlanGraphQlResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private OtpPlanResponse.Plan plan;

        public OtpPlanResponse.Plan getPlan() {
            return plan;
        }

        public void setPlan(OtpPlanResponse.Plan plan) {
            this.plan = plan;
        }
    }
}
