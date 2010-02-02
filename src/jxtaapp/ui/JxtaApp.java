package jxtaapp.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jxtaapp.service.Jxta;
import jxtaapp.service.Peer;

import org.openintents.intents.FileManagerIntents;

import peerdroid.jxta4android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * One activity for whole application
 */
public class JxtaApp extends Activity {
	public static String TAG = "AndJxta";
	private Jxta jxtaService;
	public static final Handler handler = new Handler();
	public int currentLayoutId;
	// start layout
	public static EditText txtInstanceName = null;
	public static EditText txtSeedingServer = null;
	public static Button btnStart = null;
	public static ProgressDialog dialog = null;
	// peer list layout
	public static ListView lstPeerList = null;
	public static SimpleAdapter lstPeerListAdapter = null;
	public static ArrayList<Map<String, String>> lstPeerListElements = null;
	private static final int CONTEXT_CHAT_ID = Menu.FIRST;
	private static final int CONTEXT_FILE_ID = Menu.FIRST + 1;
	// chat layout
	public static EditText txtMessage = null;
	public static Button btnSend = null;
	public static ListView lstChatHistory = null;
	public static SimpleAdapter lstChatHistoryAdapter = null;
	public static ArrayList<Map<String, String>> lstChatHistoryElements = null;
	// file layout
	public static EditText txtFilename = null;
	public static ImageButton btnOpenFilemanager = null;
	public static Button btnSendFile = null;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.start);
		currentLayoutId = R.layout.start;

		// start layout
		btnStart = (Button) findViewById(R.id.btnStart);
		txtInstanceName = (EditText) findViewById(R.id.txtInstanceName);
		txtSeedingServer = (EditText) findViewById(R.id.txtSeedingServer);

		View.OnClickListener btnStart_OnClickListener = new View.OnClickListener() {
			public void onClick(View view) {
				dialog = ProgressDialog.show(view.getRootView().getContext(),
						"", "Starting. Please wait...", true, false);

				jxtaService = new Jxta(txtInstanceName.getText().toString(),
						getFileStreamPath("jxta"), txtInstanceName.getText()
								.toString(), "", txtSeedingServer.getText()
								.toString());
				jxtaService.configureJXTA();
				try {
					jxtaService.startJXTA();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				jxtaService.startDiscovery();

				setContentView(R.layout.peer_list);
				currentLayoutId = R.layout.peer_list;

				dialog.dismiss();

				createPeerListLayout();
			}
		};
		btnStart.setOnClickListener(btnStart_OnClickListener);
	}

	/**
	 * View for peer list in the network.
	 */
	public void createPeerListLayout() {
		// peer list view
		lstPeerList = (ListView) findViewById(R.id.lstPeerList);
		lstPeerListElements = new ArrayList<Map<String, String>>();

		String[] from = { "name", "desc", "adv" };
		int[] to = { R.id.peer_list_item_name, R.id.peer_list_item_desc,
				R.id.peer_list_item_advertisement };
		lstPeerListAdapter = new SimpleAdapter(this.getApplicationContext(),
				lstPeerListElements, R.layout.peer_list_item, from, to);
		lstPeerList.setAdapter(lstPeerListAdapter);
		lstPeerList.setOnCreateContextMenuListener(this);

		for (Peer peer : jxtaService.getDiscovery().getPeerList()) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("name", peer.getName());
			map.put("desc", peer.getPipeAdvertisement().getDescription());
			map.put("adv", peer.getPipeAdvertisement().getPipeID().toString());
			JxtaApp.lstPeerListElements.add(map);
		}
		JxtaApp.lstPeerListAdapter.notifyDataSetChanged();
	}

	/**
	 * Context menu for actions on a peer.
	 * 
	 * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(android.view.ContextMenu,
	 *      android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(JxtaApp.TAG, "ContextMenu: bad ContextMenuInfo", e);
			return;
		}

		HashMap<String, String> item = (HashMap<String, String>) lstPeerListAdapter
				.getItem(info.position);
		if (item == null)
			return;

		menu.setHeaderTitle(item.get("name") + ":");

		menu.add(0, CONTEXT_CHAT_ID, 0, R.string.conChat);
		menu.add(0, CONTEXT_FILE_ID, 0, R.string.conFile);

	}

	/**
	 * If an item from the context menu is selected.
	 * 
	 * @param item
	 */
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(JxtaApp.TAG, "ContextMenu: bad ContextMenuInfo", e);
			return false;
		}

		switch (item.getItemId()) {
		case CONTEXT_CHAT_ID:
			Log.v(JxtaApp.TAG, "Menu: Start chat with: "
					+ lstPeerListElements.get(info.position).get("name"));

			setContentView(R.layout.chat);
			currentLayoutId = R.layout.chat;
			createChatLayout(jxtaService.getPeerByName(lstPeerListElements.get(
					info.position).get("name")));

			return true;
		case CONTEXT_FILE_ID:
			Log.v("ui-test", "Menu: Start file transfer with: "
					+ lstPeerListElements.get(info.position).get("name"));

			setContentView(R.layout.file);
			currentLayoutId = R.layout.file;
			createFileLayout(jxtaService.getPeerByName(lstPeerListElements.get(
					info.position).get("name")));

			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * View for a chat session with one other peer.
	 * 
	 * @param peer
	 */
	public void createChatLayout(final Peer peer) {
		txtMessage = (EditText) findViewById(R.id.txtMessage);
		btnSend = (Button) findViewById(R.id.btnSend);
		lstChatHistory = (ListView) findViewById(R.id.lstChatHistory);

		lstChatHistory
				.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		lstChatHistoryElements = new ArrayList<Map<String, String>>();

		String[] from = { "name", "time", "text" };
		int[] to = { R.id.chat_history_item_name, R.id.chat_history_item_time,
				R.id.chat_history_item_text };
		lstChatHistoryAdapter = new SimpleAdapter(this.getApplicationContext(),
				lstChatHistoryElements, R.layout.chat_history_item, from, to);
		lstChatHistory.setAdapter(lstChatHistoryAdapter);
		lstChatHistory.setOnCreateContextMenuListener(this);

		View.OnClickListener btnSend_OnClickListener = new View.OnClickListener() {
			public void onClick(View view) {
				if (jxtaService.sendMsgToPeer(peer.getName(), txtMessage
						.getText().toString(), Jxta.MessageType.TEXT)) {
					peer.addHistory("< " + peer.getName(),
							new SimpleDateFormat("dd.MM.yy HH:mm:ss")
									.format(new Date()), txtMessage.getText()
									.toString());

					Map<String, String> map = new HashMap<String, String>();
					map.put("name", "< " + peer.getName());
					map.put("time", new SimpleDateFormat("dd.MM.yy HH:mm:ss")
							.format(new Date()));
					map.put("text", txtMessage.getText().toString());
					JxtaApp.lstChatHistoryElements.add(map);
					JxtaApp.lstChatHistoryAdapter.notifyDataSetChanged();

					// txtMessage.setText("");
				} else {

				}

			}
		};
		btnSend.setOnClickListener(btnSend_OnClickListener);

		for (HashMap<String, String> item : peer.getHistory()) {
			JxtaApp.lstChatHistoryElements.add(0, item);
		}
		JxtaApp.lstChatHistoryAdapter.notifyDataSetChanged();
	}

	/**
	 * View for sending a file to another peer. Starts with a call to the file
	 * manager for selecting a specific file (<a
	 * href="http://www.openintents.org/en/node/159">OI File Manager
	 * website</a>).
	 * 
	 * @param peer
	 */
	public void createFileLayout(final Peer peer) {
		txtFilename = (EditText) findViewById(R.id.txtFilename);
		btnOpenFilemanager = (ImageButton) findViewById(R.id.btnOpenFilemanager);
		btnSendFile = (Button) findViewById(R.id.btnSendFile);

		View.OnClickListener btnOpenFilemanager_OnClickListener = new View.OnClickListener() {
			public void onClick(View view) {
				final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;

				Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
				// intent.setData(Uri.parse("file://"));
				intent.putExtra(FileManagerIntents.EXTRA_TITLE,
						getString(R.string.filemanager_open_title));
				intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
						getString(R.string.filemanager_open_button));

				try {
					startActivityForResult(intent,
							REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
				} catch (ActivityNotFoundException e) {
					// No compatible file manager was found.
					Toast.makeText(view.getRootView().getContext(),
							R.string.filemanager_not_installed,
							Toast.LENGTH_SHORT).show();
				}
			}
		};
		btnOpenFilemanager
				.setOnClickListener(btnOpenFilemanager_OnClickListener);

		View.OnClickListener btnSendFile_OnClickListener = new View.OnClickListener() {
			public void onClick(View view) {
				if (txtFilename.getText().toString().equals("")) {
					File file = new File(txtFilename.getText().toString());
					if (!file.exists())
						Toast.makeText(view.getRootView().getContext(),
								"You have to choose a file first!",
								Toast.LENGTH_LONG).show();
					return;
				}

				if (jxtaService.sendMsgToPeer(peer.getName(), txtFilename
						.getText().toString(), Jxta.MessageType.FILE)) {
					Toast.makeText(view.getRootView().getContext(),
							"File transfer was successful", Toast.LENGTH_LONG)
							.show();
					setContentView(R.layout.peer_list);
					currentLayoutId = R.layout.peer_list;

					createPeerListLayout();
				} else
					Toast.makeText(view.getRootView().getContext(),
							"ERROR while file transfer", Toast.LENGTH_LONG)
							.show();
			}
		};
		btnSendFile.setOnClickListener(btnSendFile_OnClickListener);
	}

	/**
	 * Stop also the network on activity destroy
	 */
	protected void onDestroy() {
		super.onDestroy();

		if (jxtaService != null)
			jxtaService.stop();
	}

	/**
	 * Handle the back-button to change the view and at the end close the
	 * application
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (currentLayoutId == R.layout.start
					|| currentLayoutId == R.layout.peer_list) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Are you sure you want to exit?")
						.setCancelable(false).setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										JxtaApp.this.finish();
									}
								}).setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			} else if (currentLayoutId == R.layout.chat) {
				setContentView(R.layout.peer_list);
				currentLayoutId = R.layout.peer_list;

				createPeerListLayout();
			} else if (currentLayoutId == R.layout.file) {
				setContentView(R.layout.peer_list);
				currentLayoutId = R.layout.peer_list;

				createPeerListLayout();
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * This is called after the file manager finished.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;
		txtFilename = (EditText) findViewById(R.id.txtFilename);

		switch (requestCode) {
		case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
			if (resultCode == RESULT_OK && data != null) {
				String filename = data.getDataString();
				if (filename != null) {
					// Get rid of URI prefix:
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}
					txtFilename.setText(filename);
				}
			}
			break;
		}
	}

}