package ChatClient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Client extends JFrame implements ActionListener {

	// Login GUI resource
	private JFrame login_GUI = new JFrame();
	private JPanel login_pane;
	private JTextField ip_tf;
	private JTextField port_tf;
	private JTextField id_tf;
	private JButton login_btn = new JButton("접속");

	// Main을 GUI resource
	private JPanel contentPane;
	private JTextField chat_tf;
	private JButton note_btn = new JButton("쪽지 보내기");
	private JButton join_btn = new JButton("채팅방 참여");
	private JButton create_btn = new JButton("방 만들기");
	private JButton send_btn = new JButton("전송");
	private JTextArea chat_area = new JTextArea();
	private JList user_list = new JList();
	private JList room_list = new JList();

	// Login resource
	private String ip;
	private int port;
	private String id;

	// Network resource
	private Socket sock;
	private InputStream is;
	private OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;

	// the others
	private String msg;// 메세지
	private Vector<String> user_vector = new Vector<>();// user_list에 추가 위해
	private Vector<String> room_vector = new Vector<>();// room_list에 추가 위해
	private StringTokenizer st;// inMessage 함수에서 사용하기 위한 파싱 변수
	private String myRoom = null;

	public Client() {
		loginInit();
		mainInit();
		start();
	}

	private void loginInit() {// Login GUI구성
		login_GUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		login_GUI.setBounds(100, 100, 310, 397);
		login_pane = new JPanel();
		login_pane.setBorder(new EmptyBorder(5, 5, 5, 5));
		login_GUI.setContentPane(login_pane);
		login_pane.setLayout(null);

		ip_tf = new JTextField();
		ip_tf.setBounds(103, 186, 149, 21);
		login_pane.add(ip_tf);
		ip_tf.setColumns(10);

		port_tf = new JTextField();
		port_tf.setBounds(103, 227, 149, 21);
		login_pane.add(port_tf);
		port_tf.setColumns(10);

		id_tf = new JTextField();
		id_tf.setBounds(103, 274, 149, 21);
		login_pane.add(id_tf);
		id_tf.setColumns(10);

		JLabel lblNewLabel = new JLabel("Servr IP");
		lblNewLabel.setBounds(22, 189, 62, 15);
		login_pane.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Server Port");
		lblNewLabel_1.setBounds(22, 230, 69, 15);
		login_pane.add(lblNewLabel_1);

		JLabel lblNewLabel_2 = new JLabel("ID");
		lblNewLabel_2.setBounds(41, 277, 23, 15);
		login_pane.add(lblNewLabel_2);

		login_btn.setBounds(71, 314, 153, 23);
		login_pane.add(login_btn);

		login_GUI.setVisible(true);
	}

	private void mainInit() {// Main GUI 구성
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 629, 459);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblNewLabel = new JLabel("전체 접속자");
		lblNewLabel.setBounds(12, 10, 105, 24);
		contentPane.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("채팅방 목록");
		lblNewLabel_1.setBounds(12, 213, 83, 15);
		contentPane.add(lblNewLabel_1);

		note_btn.setBounds(26, 180, 101, 23);
		contentPane.add(note_btn);

		join_btn.setBounds(22, 350, 111, 23);
		contentPane.add(join_btn);

		create_btn.setBounds(26, 383, 107, 23);
		contentPane.add(create_btn);

		chat_area.setBounds(141, 10, 462, 363);
		contentPane.add(chat_area);
		chat_area.setEditable(false);

		chat_tf = new JTextField();
		chat_tf.setBounds(145, 384, 363, 21);
		contentPane.add(chat_tf);
		chat_tf.setColumns(10);

		send_btn.setBounds(520, 383, 83, 23);
		contentPane.add(send_btn);

		user_list.setBounds(22, 44, 105, 123);
		contentPane.add(user_list);

		room_list.setBounds(22, 240, 105, 100);
		contentPane.add(room_list);
	}

	private void start() {// 메세지 시작
		login_btn.addActionListener(this);
		note_btn.addActionListener(this);
		join_btn.addActionListener(this);
		create_btn.addActionListener(this);
		send_btn.addActionListener(this);
	}

	private void network() {// network 자원 구성 -> 쓰레드를 이용
		try {
			sock = new Socket(ip, port);
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "서버를 찾지 못 하였습니다.","연결 실패", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "서버와 연결 실패","연결 실패", JOptionPane.ERROR_MESSAGE);
		}
		if (sock != null)// 성공적으로 연결 되었다면 그 다음 과정 실행
			connection();

	}

	private void connection() {// 연결 시 행동
		try {// 1.입출력 스트림 설정
			is = sock.getInputStream();
			dis = new DataInputStream(is);
			os = sock.getOutputStream();
			dos = new DataOutputStream(os);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 2.현재 접속자에 자신을 추가한다.
		sendMessage(id);
		user_vector.add(id);

		// 3. 서버 소켓과 통신 시작
		Thread th = new Thread(() -> {
			while (true) {
				try {
					msg = dis.readUTF();
				} catch (IOException e) {
					try {
						is.close();
						dis.close();
						os.close();
						dos.close();
						JOptionPane.showMessageDialog(null, "서버와의 접속이 끊어졌습니다.", "서버와 연결 실패", JOptionPane.ERROR_MESSAGE);
						break;
					} catch (IOException e1) {

					}
					e.printStackTrace();
				}
				inMessage(msg);
			}
		});
		th.start();
	}

	private void sendMessage(String str) {
		try {
			dos.writeUTF(str);
		} catch (Exception e) {
			System.out.println("메세지 전송 실패");
			e.printStackTrace();
		}
	}

	private void inMessage(String str) {// protocol에 따라 메세지 처리 -> 파싱을 위해
		st = new StringTokenizer(str, "/");
		String protocol = st.nextToken();
		String Message = st.nextToken();

		if (protocol.equals("NewUser")) {// 새로운 유저가 들어왔을 시
			user_vector.add(Message);
			Collections.sort(user_vector);
		} else if (protocol.equals("OldUser")) {
			user_vector.add(Message);
			Collections.sort(user_vector);
		} else if (protocol.equals("Note")) {
			String user = Message;
			String content = st.nextToken();
			JOptionPane.showMessageDialog(null, content, user + "로 부터 온 쪽지", JOptionPane.PLAIN_MESSAGE);
		} else if (protocol.equals("user_vector_update")) {// user_list 갱신 프로토콜
			user_list.setListData(user_vector);
		} else if (protocol.equals("CreteRoomFail")) {
			JOptionPane.showMessageDialog(null, "이미 존재하는 방 이름입니다.", "방 생성 실패", JOptionPane.ERROR_MESSAGE);
		} else if (protocol.equals("NewRoom")) {
			room_vector.add(Message);
			Collections.sort(room_vector);
			room_list.setListData(room_vector);
		} else if (protocol.equals("CreateRoom")) {
			myRoom = Message;
			create_btn.setEnabled(false);
			join_btn.setEnabled(false);
		} else if (protocol.equals("Chat")) {
			String user = Message;
			String cont = st.nextToken();
			chat_area.append(user + ": " + cont);
		} else if (protocol.equals("OriginalRoom")) {
			room_vector.add(Message);
		} else if (protocol.equals("room_vector_update")) {
			room_list.setListData(room_vector);
		} else if (protocol.equals("AccessRoom")) {
			myRoom = Message;
			create_btn.setEnabled(false);
			join_btn.setEnabled(false);
			JOptionPane.showMessageDialog(null, Message + "에 참가합니다.", "방 참가", JOptionPane.PLAIN_MESSAGE);
		} else if (protocol.equals("NewRoomUser")) {
			chat_area.append(Message + "님이 참가하셨습니다.\n");
		} else if (protocol.equals("UserOut")) {
			int idx = Collections.binarySearch(user_vector, Message);
			user_vector.remove(idx);
			Collections.sort(user_vector);
		} else if (protocol.equals("EmptyRoom")) {
			int idx = Collections.binarySearch(room_vector, Message);
			room_vector.remove(idx);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {// 핸들러 처리
		if (e.getSource() == login_btn) {
			try {
				ip = ip_tf.getText().trim();
				port = Integer.parseInt(port_tf.getText().trim());
				id = id_tf.getText().trim();
				network();
				login_GUI.setVisible(false);
				this.setVisible(true);
			} catch (Exception er) {
				System.out.println("서버 정보입력 실패");
			}
		}
		if (e.getSource() == note_btn) {
			String user = (String) user_list.getSelectedValue();
			if (user != null) {
				msg = JOptionPane.showInputDialog("보낼 쪽지 내용을 적어주세요.");
				// protocol -> Note/보낼 사람/쪽지내용
				if (msg != null) {
					msg = "Note/" + user + "/" + msg;
					sendMessage(msg);
				}
			} else {
				JOptionPane.showMessageDialog(null, "쪽지를 보낼 상대를 선택해주세요.", "쪽지 보내기 오류", JOptionPane.ERROR_MESSAGE);
			}
		}
		if (e.getSource() == join_btn) {
			String roomName = (String) room_list.getSelectedValue();
			if (roomName != null) {
				msg = "JoinRoom/" + roomName;
				sendMessage(msg);
			} else {
				JOptionPane.showMessageDialog(null, "들어갈 방을 선택해주세요.", "방 참가 오류", JOptionPane.ERROR_MESSAGE);
			}
		}
		if (e.getSource() == create_btn) {// 채팅방 생성 protocol - CreateRoom/roomName
			msg = JOptionPane.showInputDialog("채팅방 이름을 설정하세요.");
			if (msg != null) {
				if (msg.equals("")) {
					JOptionPane.showMessageDialog(null, "방 이름을 설정해 주세요.", "방 생성 실패", JOptionPane.ERROR_MESSAGE);
				} else {
					msg = "CreateRoom/" + msg;
					sendMessage(msg);
				}
			}
		}
		if (e.getSource() == send_btn) {
			msg = chat_tf.getText().trim();
			// 대화 전송 프로토콜 -Chat/속한 방이름/내용
			if (myRoom == null) {
				JOptionPane.showMessageDialog(null, "속해있는 방이 없습니다.", "전송 실패", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (msg != null) {
				msg = "Chat/" + myRoom + "/" + msg;
				chat_tf.setText("");
				sendMessage(msg);
			}
		}

	}

	public static void main(String[] args) {
		new Client();
	}
}
