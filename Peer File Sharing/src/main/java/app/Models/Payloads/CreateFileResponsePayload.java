package app.Models.Payloads;

import java.io.Serializable;
import java.util.Map;

public class CreateFileResponsePayload extends ResponsePayload implements Serializable {
    private Map<String, Integer> toBeReplicatedPeers;
    private boolean readOnly;

    private CreateFileResponsePayload(Builder builder) {
        this.statusCode = builder.statusCode;
        this.message = builder.message;
        this.toBeReplicatedPeers = builder.toBeReplicatedPeers;
        this.readOnly = builder.readOnly;
    }

    public Map<String, Integer> getToBeReplicatedPeers() {
        return toBeReplicatedPeers;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public static class Builder {
        private int statusCode;
        private String message;
        private Map<String, Integer> toBeReplicatedPeers;
        private boolean readOnly;

        public Builder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setToBeReplicatedPeers(Map<String, Integer> toBeReplicatedPeers) {
            this.toBeReplicatedPeers = toBeReplicatedPeers;
            return this;
        }

        public Builder setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public CreateFileResponsePayload build() {
            return new CreateFileResponsePayload(this);
        }
    }
}
