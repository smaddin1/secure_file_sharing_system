package app.KDS.commands;

import app.Models.Payloads.Payload;
import app.Models.Payloads.ResponsePayload;
import app.Command;

public class IllegalCommand implements Command {
    @Override
    public ResponsePayload execute(Payload payload) throws Exception {
        return new ResponsePayload.Builder()
            .setStatusCode(400)
            .setMessage("CA: Command handler not found")
            .build();
    }
}
