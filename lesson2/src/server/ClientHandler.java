package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    DataOutputStream out;
    DataInputStream in;
    private String nick;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // цикл авторизации.
                    while (true) {
                        String str = in.readUTF();
                        if(str.startsWith("/auth")){
                            String[] token = str.split(" ");
                            String newNick =
                                    AuthService.getNickByLoginAndPass(token[1],token[2]);
                            if(newNick != null){
                                if(!server.isNickAuthorized(newNick)){
                                    //   sendMsg("/authok");
                                    // сразу передадим ник клиенту
                                    sendMsg("/authok "+newNick);

                                    nick = newNick;
                                    server.subscribe(this);
                                    break;
                                }else{
                                    sendMsg("Учетная запись уже используется");
                                }
                            }else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }

                    //Цикл для работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/end");
                            System.out.println("Клиент отключился");
                            break;
                        }
                        if(str.startsWith("/w ")){
                            String[] token = str.split(" ",3);
                            server.broadcastMsg(token[2], getNick(), token[1]);
                        }else{
                            System.out.println(str);
                            server.broadcastMsg(str,getNick());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                    System.out.println("Клиент оключился");
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
