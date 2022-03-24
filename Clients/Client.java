
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

class Client {

    private DataOutputStream out;
    private BufferedReader in;
    private Socket clientsocket;
    private int port;
    private String ip;

    public Client(int port, String ip) throws IOException {
        this.port = port;
        this.ip = ip + ".csd.uoc.gr";
        this.clientsocket = new Socket(this.ip, port);
        this.out = new DataOutputStream(clientsocket.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
    }

    public void CloseConnection() {
        try {
            this.clientsocket.close();
            this.in.close();
            this.out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void Put(String key, String value) throws IOException {
        PrintMessage("I send PUT request (" + key + "," + value + ") to Server " + this.port);
        out.writeBytes("PUT /" + key + " HTTP/1.1" + "\r\n\r\n" + value + "\r\n");
        String response = in.readLine();
        PrintMessage("SERVER RESPONSE: " + response);
    }

    public void Get(String key) throws IOException {
        PrintMessage("I send GET request (" + key + ") to Server " + this.port);
        out.writeBytes("GET /" + key + " HTTP/1.1\n\n");
        String response = in.readLine();
        PrintMessage("RESPONSE from Server " + this.port + ": " + response);
    }

    public DataOutputStream getOut() {
        return out;
    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public Socket getClientsocket() {
        return clientsocket;
    }

    public void setClientsocket(Socket clientsocket) {
        this.clientsocket = clientsocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void PrintMessage(String message) {
        System.out.println("Client: " + message);
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Client client = null;
        ArrayList<String> computers = new ArrayList<String>(Arrays.asList("kiwi", "frapa", "firiki"));
        int[] ports = {3980, 3981, 3982};
        while (true) {
            System.out.println("------------------------------");
            String method = "", computer = "", key = "", value = "";
            int port = 0;
            System.out.print("Computer: ");
            computer = scanner.nextLine();
            if (!computers.contains(computer)) {
                System.out.println("Wrong computer");
                continue;
            }
            port = ports[computers.indexOf(computer)];
            System.out.print("Method: ");
            method = scanner.nextLine().toUpperCase();
            System.out.print("Key: ");
            key = scanner.nextLine();

            try {
                switch (method) {
                    case "PUT":
                        System.out.print("Value: ");
                        value = scanner.nextLine();
                        client = new Client(port, computer);
                        client.Put(key, value);
                        break;
                    case "GET":
                        client = new Client(port, computer);
                        client.Get(key);
                        break;
                    default:
                        System.out.println("ERROR: Wrong method");
                        continue;
                }
                client.CloseConnection();
            } catch (ConnectException ex) {
                System.out.println("I couldn't connect to Server " + client.getPort());
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
                client.CloseConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
                client.CloseConnection();
            } finally {
                continue;
            }

        }

    }
}
