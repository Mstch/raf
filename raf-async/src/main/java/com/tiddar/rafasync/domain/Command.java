package com.tiddar.rafasync.domain;


public class Command {
    public String opt;
    public String key;
    public String value;

    @Override
    public String toString() {
        return opt + " " + key + (value == null || value.equals("") ? "" : " " + value);
    }

    public Log toLog() {
        Log log = new Log();
        log.setCommand(toString());
        return log;
    }

    public static Command fromString(String src) {
        String[] attrs = src.split(" ");
        Command command = new Command();
        command.opt = attrs[0].toLowerCase();
        command.key = attrs[1].toLowerCase();
        command.value = attrs[2].toLowerCase();
        return command;
    }

    public String apply() {
        if ("get".equals(opt)) {
            return FSM.state.get(key);
        } else if ("set".equals(opt)) {
            return FSM.state.put(key, value);
        } else if ("remove".equals(opt)) {
            return FSM.state.remove(key);
        }
        return "";
    }
}
