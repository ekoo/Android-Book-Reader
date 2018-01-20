package com.github.axet.bookreader.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.axet.bookreader.R;
import com.github.axet.bookreader.activities.MainActivity;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.widgets.FBReaderView;

import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;

import java.util.ArrayList;
import java.util.List;

public class ReaderFragment extends Fragment {
    public static final String TAG = ReaderFragment.class.getSimpleName();

    Storage storage;
    FBReaderView view;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (a.equals(MainActivity.ACTION_SEARCH))
                view.app.runAction(ActionCode.SEARCH, intent.getStringExtra("search"));
            if (a.equals(MainActivity.ACTION_SEARCH_CLOSE))
                view.app.hideActivePopup();
        }
    };

    BroadcastReceiver battery = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            view.battery = level * 100 / scale;
        }
    };

    public class TOCAdapter implements ListAdapter {
        ArrayList<TOCTree> tree = new ArrayList<>();

        public TOCAdapter(List<TOCTree> ll) {
            loadTOC(ll);
        }

        void loadTOC(List<TOCTree> tree) {
            for (TOCTree t : tree) {
                this.tree.add(t);
                loadTOC(t.subtrees());
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return tree.size();
        }

        @Override
        public TOCTree getItem(int position) {
            return tree.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.toc_item, null);
            }
            TOCTree tt = tree.get(position);
            TextView t = (TextView) convertView.findViewById(R.id.text);
            t.setText(tt.getText());
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return tree.isEmpty();
        }
    }

    public ReaderFragment() {
    }

    public static ReaderFragment newInstance(Uri uri) {
        ReaderFragment fragment = new ReaderFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(getContext());
        setHasOptionsMenu(true);
        IntentFilter ff = new IntentFilter(MainActivity.ACTION_SEARCH);
        ff.addAction(MainActivity.ACTION_SEARCH_CLOSE);
        getContext().registerReceiver(receiver, ff);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).clearMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reader, container, false);

        final MainActivity main = (MainActivity) getActivity();

        view = (FBReaderView) v.findViewById(R.id.main_view);

        Context context = getContext();
        context.registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        view.setWindow(getActivity().getWindow());
        view.setActivity(getActivity());

        Uri uri = getArguments().getParcelable("uri");

        try {
            Storage.Book b = storage.load(uri);
            if (!b.isLoaded())
                storage.load(b);
            view.loadBook(b);
        } catch (RuntimeException e) {
            main.Error(e);
            main.openLibrary();
        }

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        savePosition();
    }

    void savePosition() {
        if (view.book == null)
            return;
        view.book.info.position = view.getPosition();
        storage.save(view.book);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getContext();
        context.unregisterReceiver(battery);
        context.unregisterReceiver(receiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toc) {
            showTOC();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final MainActivity main = (MainActivity) getActivity();
        main.homeMenu.setVisible(false);
        main.tocMenu.setVisible(view.app.Model.TOCTree != null && view.app.Model.TOCTree.hasChildren());
    }

    void showTOC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        ArrayList<CharSequence> items = new ArrayList<>();
        final TOCAdapter a = new TOCAdapter(view.app.Model.TOCTree.subtrees());
        builder.setSingleChoiceItems(a, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                view.gotoPosition(a.getItem(which).getReference());
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
}
