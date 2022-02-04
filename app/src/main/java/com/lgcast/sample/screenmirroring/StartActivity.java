package com.lgcast.sample.screenmirroring;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;

public class StartActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<HashMap<String, String>> items = new ArrayList<>();

        items.add(addMenuItem(getString(R.string.menu_title_mirroring), getString(R.string.menu_desc_mirroring)));
        items.add(addMenuItem(getString(R.string.menu_title_dual_screen), getString(R.string.menu_desc_dual_screen)));

        String[] from = new String[]{"title", "desc"};
        int[] to = new int[]{android.R.id.text1, android.R.id.text2};
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, items, android.R.layout.simple_list_item_2, from, to);
        setListAdapter(simpleAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (position == 0) startActivity(new Intent(this, ScreenMirroringActivity.class));
        if (position == 1) startActivity(new Intent(this, DualFirstScreenActivity.class));
    }

    private HashMap<String, String> addMenuItem(String title, String desc) {
        HashMap<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("desc", desc);
        return item;
    }
}
