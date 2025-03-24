import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class VMWriter {

    private BufferedWriter writer;
    private boolean isFirstCommand;

    public VMWriter(String outputFile) throws IOException {
        writer = new BufferedWriter(new FileWriter(outputFile));
        isFirstCommand = true;
    }

    public void writePush(Segment segment, int index) throws IOException {
        String str;
        switch (segment) {
            case CONST:
                str = "constant";
                break;
            case ARG:
                str = "argument";
                break;
            default:
                str = segment.toString().toLowerCase();
                break;
        }
        writer.write("\n" + "    push " + str + " " + index);
        writer.flush();
    }

    public void writePop(Segment segment, int index) throws IOException {
        String str;
        switch (segment) {
            case CONST:
                str = "constant";
                break;
            case ARG:
                str = "argument";
                break;
            default:
                str = segment.toString().toLowerCase();
                break;
        }
        writer.write("\n" + "    pop " + str + " " + index);
        writer.flush();
    }

    public void writeArithmetic(Command command) throws IOException {
        if (command == Command.NEG) {
            writer.write("\n" + "    neg");
        } else {
            writer.write("\n" + "    " + command.toString().toLowerCase());
        }
        writer.flush();
    }

    public void writeLabel(String label) throws IOException {
        writer.write("\n" + "label " + label);
        writer.flush();
    }

    public void writeGoto(String label) throws IOException {
        writer.write("\n" + "    goto " + label);
        writer.flush();
    }

    public void writeIf(String label) throws IOException {
        writer.write("\n" + "    if-goto " + label);
        writer.flush();
    }

    public void writeCall(String name, int nArgs) throws IOException {
        writer.write("\n" + "    call " + name + " " + nArgs);
        writer.flush();
    }

    public void writeFunction(String name, int nLocals) throws IOException {
        String command = "";
        if (isFirstCommand) {
            command = "function " + name + " " + nLocals;
            isFirstCommand = false;
        } else {
            command = "\n" + "function " + name + " " + nLocals;
        }
        writer.write(command);
        writer.flush();
    }

    public void writeReturn() throws IOException {
        writer.write("\n" + "    return");
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }

}
