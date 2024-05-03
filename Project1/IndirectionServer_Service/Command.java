/**
 * Author Joshua Wolfel
 */
import java.util.ArrayList;
import java.util.List;

public class Command {
    private List<String> args = new ArrayList<String>();
    private String name = "";

    Command(String request) {
        String[] command;
        if (request != null) {
            command = request.trim().split("\\s+");
            for (int i = 0; i < command.length; i+=1) {
                if (i == 0) {
                    this.name = command[i];
                } else {
                    this.args.add(command[i]);
                }
            }
        }
    }

    public String getCommandName() {
        return this.name;
    }

    public String[] getArgs() {
        return args.toArray(new String[args.size()]);
    }

    public String argsAsString() {
        StringBuilder arg_string = new StringBuilder();
        args.forEach((arg) -> arg_string.append(arg + " "));
        return arg_string.toString();
    }
}
