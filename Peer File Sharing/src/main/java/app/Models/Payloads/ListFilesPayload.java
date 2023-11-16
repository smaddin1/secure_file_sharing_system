package app.Models.Payloads;

import app.Models.PeerInfo;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ListFilesPayload extends Payload implements Serializable {
    private final String pwd;

    public ListFilesPayload(Builder builder) {
        super();
        this.command = builder.command;
        this.peerInfo = builder.peerInfo;
        this.pwd = builder.pwd;
    }

    public String getPwd() {
        return pwd;
    }

    public static class Builder {
        private String command;
        private PeerInfo peerInfo;
        private String pwd;

        public Builder setCommand(String command) {
            this.command = command;
            return this;
        }

        public Builder setPeerInfo(PeerInfo peerInfo) {
            this.peerInfo = peerInfo;
            return this;
        }

        public Builder setPwd(String pwd) {
            Path path = Paths.get(pwd).normalize();
            this.pwd = path.toString();
            return this;
        }

        public ListFilesPayload build() {
            return new ListFilesPayload(this);
        }
    }
}
