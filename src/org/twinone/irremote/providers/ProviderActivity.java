package org.twinone.irremote.providers;

import org.twinone.androidlib.NavigationFragment.NavigationListener;
import org.twinone.irremote.R;
import org.twinone.irremote.components.AnimHelper;
import org.twinone.irremote.components.Button;
import org.twinone.irremote.components.Remote;
import org.twinone.irremote.ir.Signal;
import org.twinone.irremote.ir.io.Transmitter;
import org.twinone.irremote.providers.common.CommonProviderFragment;
import org.twinone.irremote.providers.common.CommonProviderFragment.CommonProviderData;
import org.twinone.irremote.providers.globalcache.GCProviderFragment;
import org.twinone.irremote.providers.globalcache.GlobalCacheProviderData;
import org.twinone.irremote.providers.learn.LearnRemoteProviderFragment;
import org.twinone.irremote.providers.lirc.LircProviderData;
import org.twinone.irremote.providers.lirc.LircProviderFragment;
import org.twinone.irremote.ui.ProviderNavFragment;
import org.twinone.irremote.ui.dialogs.SaveButtonDialog;
import org.twinone.irremote.ui.dialogs.SaveButtonDialog.OnSaveButton;
import org.twinone.irremote.ui.dialogs.SaveRemoteDialog;
import org.twinone.irremote.ui.dialogs.SaveRemoteDialog.OnRemoteSavedListener;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

public class ProviderActivity extends ActionBarActivity implements
		NavigationListener {

	private String mAction;

	public String getAction() {
		return mAction;
	}

	/**
	 * The user will select a remote which will be saved directly
	 */
	public static final String ACTION_SAVE_REMOTE = "org.twinone.irremote.intent.action.save_remote";

	/**
	 * The user will select a button that will be returned to the calling
	 * activity
	 */
	public static final String ACTION_GET_BUTTON = "org.twinone.irremote.intent.action.get_button";

	/**
	 * This extra contains a Button object representing the button that the user
	 * has chosen
	 */
	public static final String EXTRA_RESULT_BUTTON = "org.twinone.irremote.intent.extra.result_buttons";

	/**
	 * If specified, {@link ProviderActivity} will open this provider instead of
	 * the default
	 */
	public static final String EXTRA_PROVIDER = "org.twinone.irremote.intent.extra.provider_name";

	/** Common remotes (assets db) */
	public static final int PROVIDER_COMMON = 1;
	/** Lirc online database */
	public static final int PROVIDER_LIRC = 2;
	/** GlobalCaché online database */
	public static final int PROVIDER_GLOBALCACHE = 3;
	/** Twinone online database */
	public static final int PROVIDER_TWINONE = 4;
	/** On HTC Devices, learn a remote (or button) */
	public static final int PROVIDER_LEARN = 5;
	/** My remotes */
	public static final int PROVIDER_LOCAL = 6;

	/** Provides an empty remote (no buttons) or button (no code, color or text) */
	public static final int PROVIDER_EMPTY = 7;

	private Transmitter mTransmitter;

	private int mInnerFragmentCurrentState;
	private int mInnerFragmentExitState;

	private ProviderNavFragment mNavFragment;

	private int mCurrentProvider;
	private Toolbar mToolbar;

	public Toolbar getToolbar() {
		return mToolbar;
	}

	public void setCurrentState(int state) {
		mInnerFragmentCurrentState = state;
	}

	public void setExitState(int state) {
		mInnerFragmentExitState = state;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getAction() == null) {
			throw new IllegalStateException(
					"ProviderActivity should be called with one of ACTION_SAVE_REMOTE of ACTION_GET_BUTTON specified");
		}

		mAction = getIntent().getAction();

		setContentView(R.layout.activity_provider);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		mToolbar.inflateMenu(R.menu.db_menu);

		setupNavigation();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(SAVE_TITLE)) {
				setTitle(savedInstanceState.getString(SAVE_TITLE));
			}
		} else {
			int provider = getIntent().getIntExtra(EXTRA_PROVIDER,
					PROVIDER_COMMON);
			switchTo(provider);
		}

		setTitle(R.string.app_name);
		mNavFragment.open(true);
	}

	private void setupNavigation() {
		mNavFragment = (ProviderNavFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mNavFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		mNavFragment.setEdgeSizeDp(30);
		mNavFragment.setNavigationListener(this);

	}

	/**
	 * Use this method to send the selected button back to the calling activity
	 */

	public void saveButton(final Button button) {

		// LayoutInflater li = LayoutInflater.from(this);
		// View v = li.inflate(R.layout.dialog_save_button, null);
		// final ButtonView bv = (ButtonView) v
		// .findViewById(R.id.dialog_save_button_button);
		// bv.setButton(button);
		// if (getTransmitter() != null)
		// bv.setOnTouchListener(new TransmitOnTouchListener(getTransmitter()));
		//
		// final AlertDialog.Builder ab = new AlertDialog.Builder(this);
		// ab.setTitle(R.string.save_button_dlgtit);
		// ab.setMessage(R.string.save_button_dlgmsg);
		// ab.setView(v);
		// ab.setNegativeButton(android.R.string.cancel, null);
		// ab.setPositiveButton(android.R.string.ok,
		// new DialogInterface.OnClickListener() {
		//
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// Intent i = new Intent();
		// i.putExtra(EXTRA_RESULT_BUTTON, button);
		// setResult(Activity.RESULT_OK, i);
		// finish();
		// }
		// });
		// AnimHelper.showDialog(ab);
		SaveButtonDialog d = SaveButtonDialog.newInstance(button);
		d.setListener(new OnSaveButton() {

			@Override
			public void onSaveButton(Button result) {
				Intent i = new Intent();
				i.putExtra(EXTRA_RESULT_BUTTON, result);
				setResult(Activity.RESULT_OK, i);
				finish();
			}
		});
		d.show(this);
	}

	/**
	 * Use this method to prompt the user to save this remote
	 * 
	 * @param r
	 */
	public void saveRemote(Remote remote) {
		// SaveRemoteDialog dialog = SaveRemoteDialog.newInstance(remote);
		// dialog.setListener(new OnRemoteSavedListener() {
		//
		// @Override
		// public void onRemoteSaved(String name) {
		// // Finish the activity, we've saved the remote
		// Remote.setLastUsedRemoteName(ProviderActivity.this, name);
		// Toast.makeText(ProviderActivity.this,
		// R.string.remote_saved_toast, Toast.LENGTH_SHORT).show();
		// finish();
		// }
		// });
		// dialog.show(this);
		saveRemote(this, remote);
	}

	private static void saveRemote(final Activity activity, Remote remote) {
		SaveRemoteDialog dialog = SaveRemoteDialog.newInstance(remote);
		dialog.setListener(new OnRemoteSavedListener() {

			@Override
			public void onRemoteSaved(String name) {
				// Finish the activity, we've saved the remote
				Remote.setLastUsedRemoteName(activity, name);
				Toast.makeText(activity, R.string.remote_saved_toast,
						Toast.LENGTH_SHORT).show();
				activity.finish();
			}
		});
		dialog.show(activity);
	}

	@Override
	public void onBackPressed() {
		if (mInnerFragmentCurrentState == mInnerFragmentExitState) {
			finish();
		} else {
			getFragmentManager().popBackStack();
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		return onNavigateUp();
	}

	@Override
	public boolean onNavigateUp() {
		Log.d("TAG", "onnavigateup");

		if (mInnerFragmentCurrentState == mInnerFragmentExitState) {
			finish();
		} else {
			getFragmentManager().popBackStack();
		}
		return true;
	}

	private String mTitle;
	private static final String SAVE_TITLE = "save_title";

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		if (mNavFragment.isOpen()) {
			mSavedTitle = title.toString();
		} else {
			getSupportActionBar().setTitle(title);
			mTitle = (String) title;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVE_TITLE, mTitle);
	}

	public void transmit(Signal signal) {
		getTransmitter().transmit(signal);
	}

	public void addFragment(ProviderFragment fragment) {
		Log.w("ProviderActivity", "Adding fragment!");
		getFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).addToBackStack("default")
				.commit();
	}

	public Transmitter getTransmitter() {
		// Lazy initialization
		if (mTransmitter == null) {
			mTransmitter = Transmitter.getInstance(this);
		}
		return mTransmitter;
	}

	@Override
	public void finish() {
		super.finish();
		AnimHelper.onFinish(this);
	}

	public void addCommonProviderFragment(CommonProviderData data) {
		setExitState(CommonProviderData.TARGET_DEVICE_TYPE);

		mInnerFragmentCurrentState = data.targetType;
		CommonProviderFragment frag = new CommonProviderFragment();
		Bundle args = new Bundle();
		args.putSerializable(CommonProviderFragment.ARG_DATA, data);
		frag.setArguments(args);
		addFragment(frag);
	}

	public void addGCProviderFragment(GlobalCacheProviderData data) {
		setExitState(CommonProviderData.TARGET_DEVICE_TYPE);

		mInnerFragmentCurrentState = data.targetType;
		GCProviderFragment frag = new GCProviderFragment();
		Bundle args = new Bundle();
		args.putSerializable(GCProviderFragment.ARG_URI_DATA, data);
		frag.setArguments(args);
		addFragment(frag);
	}

	public void addLircProviderFragment(LircProviderData data) {
		mInnerFragmentCurrentState = data.targetType;
		LircProviderFragment frag = new LircProviderFragment();
		Bundle args = new Bundle();
		args.putSerializable(LircProviderFragment.ARG_URI_DATA, data);
		frag.setArguments(args);
		addFragment(frag);
	}

	public void popAllFragments() {
		getFragmentManager().popBackStack(null,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
	}

	public void popFragment() {
		getFragmentManager().popBackStack();
	}

	int mPendingSwitch = -1;

	public void switchTo(int provider) {
		if (provider == mCurrentProvider)
			return;
		if (mNavFragment.isOpen()) {
			mPendingSwitch = provider;
			mNavFragment.close();
		} else {
			switchToImpl(provider);
		}
	}

	private void switchToImpl(int provider) {
		Log.d("", "SwitchToImpl");
		popAllFragments();
		switch (provider) {
		case PROVIDER_GLOBALCACHE:
			addFragment(new GCProviderFragment());
			break;
		case PROVIDER_LEARN:
			addFragment(new LearnRemoteProviderFragment());
			break;
		default:
			addFragment(new CommonProviderFragment());
			break;
		}

		mCurrentProvider = provider;
	}

	private String mSavedTitle;

	@Override
	public void onNavigationOpened() {
		Log.i("", "OnNavigationOpened");
		mSavedTitle = getTitle().toString();
		if (ACTION_GET_BUTTON.equals(mAction)) {
			getSupportActionBar().setTitle(R.string.title_provider_add_button);
		} else {
			getSupportActionBar().setTitle(R.string.title_provider_add_remote);
		}
	}

	@Override
	public void onNavigationClosed() {
		Log.i("", "OnNavigationCLosed");
		getSupportActionBar().setTitle(mSavedTitle);

		if (mPendingSwitch != -1) {
			switchToImpl(mPendingSwitch);
			mPendingSwitch = -1;
		}
	}

}
