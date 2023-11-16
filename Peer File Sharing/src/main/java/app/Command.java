package app;

import app.Models.Payloads.Payload;
import app.Models.Payloads.ResponsePayload;

public interface Command {
    ResponsePayload execute(Payload payload) throws Exception;
}
