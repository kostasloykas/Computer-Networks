//CSD3980 KONSTANTINOS LOUKAS
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler implements Runnable {

    private Socket clientSocket;
    private WebServer webServer;
    private DataOutputStream out;
    private BufferedReader in;

    public ClientHandler(Socket clientSocket, WebServer webServer) throws IOException {
        this.clientSocket = clientSocket;
        this.webServer = webServer;
        this.out = new DataOutputStream(clientSocket.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void Close_connection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        try {
            String firstline = in.readLine();
//          logo tou healthcheck
            if (firstline == null) {
                return;
            }
            StringTokenizer st = new StringTokenizer(firstline, " ");
            String method = st.nextToken();
            String key = st.nextToken().replace("/", "");

            String line;
            line = in.readLine();
            String port = "";
            String headerline = "";
            if (line.contains("From")) {
                headerline = line;
                st = new StringTokenizer(line, ":");
                while (st.hasMoreTokens()) {
                    port = st.nextToken();
                }
            }

            if (!headerline.equals("")) {
                webServer.PrintMessage("New request " + method + " from server " + port);
            } else {
                webServer.PrintMessage("New request " + method + " from  client");
            }

            if (method.equals("ASK")) {
                line = in.readLine();
                if (line.contains("YES")) {
                    String server = "";
                    line = in.readLine();
                    st = new StringTokenizer(line, ":");
                    st.nextToken();
                    server = st.nextToken();
                    line = in.readLine();
                    String value = in.readLine(); // periexei to value
                    HashMap<String, String> storage = webServer.getStorage();
                    storage.put(key, value);
                    webServer.setStorage(storage);
                    if (headerline.contains(webServer.getId().toString())) {
                        webServer.PrintMessage("I took value from Server" + server);
                        return;
                    } else {
                        ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "ASK /" + key + " HTTP/1.1 \n" + headerline + ":" + webServer.getId() + "\nFOUND:YES\nFOUND-FROM:" + server + "\n\n" + value + "\n");
                        new Thread(tmp).start();
                    }
                } else {
                    if (headerline.contains(webServer.getId().toString())) {
                        return;
                    }
                    String value = "";
                    if ((value = webServer.Get(key)) == null) {
                        ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "ASK /" + key + " HTTP/1.1 \n" + headerline + ":" + webServer.getId() + "\nFOUND:NO\n");
                        new Thread(tmp).start();
                    } else {
                        ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "ASK /" + key + " HTTP/1.1 \n" + headerline + ":" + webServer.getId() + "\nFOUND:YES\nFOUND-FROM:" + webServer.getId() + "\n\n" + value + "\n");
                        new Thread(tmp).start();
                    }
                }

            } else if (method.equals("GET")) {
                String value = "";
                if ((value = webServer.Get(key)) == null) {
                    ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "ASK /" + key + " HTTP/1.1 \nFrom:" + webServer.getId() + "\nFOUND:NO\n");
                    new Thread(tmp).start();
                    Thread.sleep(2500); // 8a perimenoume 2 deuterolepta mexri na er8ei apanthsh
                    if ((value = webServer.Get(key)) == null) {
                        out.writeBytes("The key doesn't exists");
                    } else {
                        out.writeBytes("The value of (" + key + ") is " + value + "\n");
                    }

                } else {
                    out.writeBytes("The value of (" + key + ") is " + value + "\n");
                }

            } else if (method.equals("PUT")) {

                if (!line.isEmpty()) {
                    while (!in.readLine().isEmpty()) {
                    }
                }


                String value = in.readLine().replace("\n", "");
                webServer.Put(key, value);
                out.writeBytes("HTTP PUT complete for (" + key + "," + value + ")\n");

                if (headerline.equals("")) {
                    if (webServer.getNext() == 0) {
                        return;
                    }
                    ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "PUT /" + key + " HTTP/1.1 \nFrom:" + webServer.getId().toString() + "\n" + "\n" + value + "\n");
                    new Thread(tmp).start();
                } else {
                    ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "PUT /" + key + " HTTP/1.1 \n" + headerline + ":" + webServer.getId().toString() + "\n" + "\n" + value + "\n");
                    new Thread(tmp).start();
                }

            } else if (method.equals("JOIN")) {
                assert !port.equals("") : "port=\"\"";
                assert !headerline.equals("") : "headerline=\"\"";

                line = in.readLine();// domain
                st = new StringTokenizer(line, ":");
                st.nextToken();
                String domain = st.nextToken();

                LinkedList<Integer> list = webServer.getList();
                LinkedList<String> names = webServer.getNames();
                list.add(list.indexOf(webServer.getId()) + 1, Integer.parseInt(port));
                names.add(list.indexOf(webServer.getId()) + 1, domain);
                webServer.setList(list);
                webServer.setNames(names);
                webServer.setNext(Integer.parseInt(port));
                webServer.setNext_ip(domain);

                out.writeBytes("Accepted");

                ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "UPDATELIST /URL HTTP/1.1 \nFrom:" + webServer.getId() + "\n" + "\n" + webServer.ListToString() + "\n" + webServer.NamesToString() + "\n");
                new Thread(tmp).start();

            } else if (method.equals("UPDATELIST")) {
                assert !port.equals("") : "port=\"\"";
                assert !headerline.equals("") : "headerline=\"\"";

                String x;
                while (!(x = in.readLine()).isEmpty()) {
                }

                line = in.readLine(); // h lista me ta ports twn servers
                st = new StringTokenizer(line, " ");
                LinkedList<Integer> list = new LinkedList<Integer>();
                while (st.hasMoreTokens()) {
                    int server_port = Integer.parseInt(st.nextToken());
                    list.add(server_port);
                }
                webServer.setList(list);

                line = in.readLine(); // h lista me ta domain name twn servers
                st = new StringTokenizer(line, " ");
                LinkedList<String> names = new LinkedList<String>();
                while (st.hasMoreTokens()) {
                    String domain = st.nextToken().toString();
                    names.add(domain);
                }
                webServer.setNames(names);

                list = webServer.getList();
                if (list.getLast().equals(webServer.getId())) {
                    webServer.setNext(list.getFirst());
                    webServer.setNext_ip(names.getFirst());
                } else {
                    webServer.setNext(list.get(list.indexOf(webServer.getId()) + 1));
                    webServer.setNext_ip(names.get(list.indexOf(webServer.getId()) + 1));
                }

                ServerHandler tmp = new ServerHandler(webServer.getNext(), webServer.getNext_ip(), webServer, "UPDATELIST /URL HTTP/1.1 \n" + headerline + ":" + webServer.getId().toString() + "\n" + "\n" + webServer.ListToString() + "\n" + webServer.NamesToString() + "\n");
                new Thread(tmp).start();

            } else {
                webServer.PrintMessage("Not supported method");
            }

            this.Close_connection();

        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            this.Close_connection();
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}

class ServerHandler implements Runnable {

    private Socket clientSocket;
    private WebServer webServer;
    private DataOutputStream out;
    private BufferedReader in;
    private String message;
    private int port;
    private String ip;


    public ServerHandler(int port, String ip, WebServer webServer, String message) {
        try {
            this.port = port;
            this.ip = ip;
            this.clientSocket = new Socket(this.ip, port);
            this.webServer = webServer;
            this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.message = message;
        } catch (ConnectException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void Close_connection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void run() {

        try {
            if (message == null) {
                return;
            }
            StringTokenizer st = new StringTokenizer(message, " ");
            String method = st.nextToken();
            String lines[] = message.split(System.lineSeparator());

            if (lines[1].contains(String.valueOf(webServer.getNext())) && !method.equals("ASK")) {
                return;
            }
            webServer.PrintMessage("I send request " + method + " to server " + this.port);
            if (method.equals("UPDATELIST")) {
                out.writeBytes(message);
            } else if (method.equals("PUT")) {
                out.writeBytes(message);
            } else if (method.equals("JOIN")) {
                out.writeBytes(message);
                String response = in.readLine();

                if (response.equals("Accepted")) {
                    webServer.PrintMessage("Accepted to join servers group");
                } else {
                    webServer.PrintMessage("Rejected to join servers group");
                }
            } else if (method.equals("ASK")) {
                out.writeBytes(message);
            } else {
                webServer.PrintMessage("Not supported method");
            }

        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }



    }

}


class WebServer implements Runnable {

    private Integer id;
    private String ip;
    private int next;
    private String next_ip;
    private LinkedList<Integer> list;
    private LinkedList<String> names;
    private ServerSocket serverSocket;
    private HashMap<String, String> storage;
    private PrintWriter out;
    private BufferedReader in;

    public WebServer(int id, String ip) throws IOException {
        this.id = id;
        this.ip = ip;
        this.list = new LinkedList<Integer>();
        this.names = new LinkedList<String>();
        this.names.add(this.ip);
        this.list.add(this.id);
        this.storage = new HashMap<String, String>();
        this.serverSocket = new ServerSocket(id);
        this.next = 0;
        this.next_ip = null;
    }

    public void StartServer() throws IOException {
        PrintMessage("Server Started");
        ClientHandler client;
        new Thread(this).start();//health check
        while (true) {
            client = new ClientHandler(serverSocket.accept(), this);
            new Thread(client).start();
        }
    }

    public String Get(String key) {
        if (storage.containsKey(key)) {
            return storage.get(key);
        }
        return null;
    }

    public void Put(String key, String value) {
        storage.put(key, value);
        PrintMessage("New entry -> (" + key + "," + value + ") Storage updated: " + StorageToString());
    }

    public void RequestJoin(int port, String ip) {

        try {
            ServerHandler tmp = new ServerHandler(port, ip, this, "JOIN /URL HTTP/1.1 \nFrom:" + this.id + "\nIP:" + this.ip + "\n");
            new Thread(tmp).start();
            this.StartServer();
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void CloseServer() throws IOException {
        this.in.close();
        this.out.close();
        this.serverSocket.close();

    }

    public void PrintMessage(String message) {
        System.out.println("Server port(" + this.id + "): " + message);
    }

    public String StorageToString() {
        String str = "";
        Iterator<Map.Entry<String, String>> iter = storage.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> mapElement = iter.next();
            str += "(" + mapElement.getKey() + "," + mapElement.getValue() + ")   ";
        }
        return str;
    }

    public String ListToString() {
        String str = "";
        Iterator<Integer> iter = list.iterator();
        while (iter.hasNext()) {
            str += iter.next().toString() + " ";
        }
        return str;
    }

    public String NamesToString() {
        String str = "";
        Iterator<String> iter = names.iterator();
        while (iter.hasNext()) {
            str += iter.next().toString() + " ";
        }
        return str;
    }

    //    health check
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1500);
                if (next == 0) {
                    continue;
                }
                Socket nextServer = new Socket(this.next_ip, next);
                nextServer.close();

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (ConnectException ex) {
                PrintMessage("I lost the connection with server in port " + next);

                assert list.size() >= 2 : "last.size()>=2";
                assert next != 0 : "next!=0";

                if (list.size() == 2) {
                    list.remove(list.indexOf(next));
                    names.remove(names.indexOf(next_ip));
                    next = 0;
                    next_ip = null;
                } else {
                    list.remove(list.indexOf(next));
                    names.remove(names.indexOf(next_ip));
                    if (list.indexOf(this.id) == list.size() - 1) {
                        next = list.getFirst();
                        next_ip = names.getFirst();
                    } else {
                        next = list.get(list.indexOf(this.id) + 1);
                        next_ip = names.get(list.indexOf(this.id) + 1);
                    }
                    //Update list request
                    ServerHandler tmp = new ServerHandler(next, this.next_ip, this, "UPDATELIST /URL HTTP/1.1 \nFrom:" + this.getId() + "\n" + "\n" + this.ListToString() + "\n" + this.NamesToString() + "\n");
                    new Thread(tmp).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public static void main(String argv[]) throws Exception {

       WebServer server = new WebServer(3981, "frapa.csd.uoc.gr");
       server.RequestJoin(3980, "kiwi.csd.uoc.gr");


    }

    public LinkedList<String> getNames() {
        return names;
    }

    public void setNames(LinkedList<String> names) {
        this.names = names;
        this.PrintMessage("Names updated: " + this.NamesToString());
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNext_ip() {
        return next_ip;
    }

    public void setNext_ip(String next_ip) {
        this.next_ip = next_ip;
    }
    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public LinkedList<Integer> getList() {
        return list;
    }

    public void setList(LinkedList<Integer> list) {
        this.list = list;
        this.PrintMessage("List updated: " + this.ListToString());

    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public HashMap<String, String> getStorage() {
        return storage;
    }

    public void setStorage(HashMap<String, String> storage) {
        this.storage = storage;
    }
}
