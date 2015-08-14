package sg.edu.nus.iss.smartpantry.application;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import sg.edu.nus.iss.smartpantry.CustomException.ItemNotFoundException;
import sg.edu.nus.iss.smartpantry.Entity.Item;
import sg.edu.nus.iss.smartpantry.Entity.Product;
import sg.edu.nus.iss.smartpantry.R;
import sg.edu.nus.iss.smartpantry.application.network.ItemLookup;
import sg.edu.nus.iss.smartpantry.application.util.CustomAdapter;
import sg.edu.nus.iss.smartpantry.application.util.NotificationService;
import sg.edu.nus.iss.smartpantry.controller.ControlFactory;
import sg.edu.nus.iss.smartpantry.controller.MainController;
import sg.edu.nus.iss.smartpantry.views.ExpiringItems;
import sg.edu.nus.iss.smartpantry.views.ItemDetails;
import sg.edu.nus.iss.smartpantry.views.ShopCreateActivity;
import sg.edu.nus.iss.smartpantry.views.fragments.HomePageFragment;

//import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
//import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
//import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;


public class SPApp extends ActionBarActivity{
    private MainController mainController;
    List<Product> productList = new ArrayList<Product>();
    List<Item> prodItemList = new ArrayList<Item>();
    List<List<Item>> itemList = new ArrayList<List<Item>>();
    ExpandableListView expListView;
    CustomAdapter customAdapter;
    private static final int CAMERA_REQUEST = 1888;
    int lastExpandedGroupPosition;
    //private MobileServiceClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spapp);
        setActivityBackgroundColor("#FFFFFF");

        if (savedInstanceState == null) {
            HomePageFragment homePageFragment= new HomePageFragment();
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();;
            fragmentTransaction.add(R.id.listContainer, homePageFragment, "HomePage");
            fragmentTransaction.commit();
        }
        //Azure deployment
//        try {
//            mClient = new MobileServiceClient(
//                    "https://isscourse.azure-mobile.net/",
//                    "krwsNiMRCExqanRaHcVWvocQKdLjbk62",
//                    this
//            );
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        TestTable item = new TestTable();
//        item.Text = "Awesome item";
//        mClient.getTable(TestTable.class).insert(item, new TableOperationCallback<TestTable>() {
//            @Override
//            public void onCompleted(TestTable entity, Exception exception, ServiceFilterResponse response) {
//                if (exception == null) {
//                    // Insert succeeded
//                } else {
//                    // Insert failed
//                }
//            }
//        });

        //lastExpandedGroupPosition=-1;

        //Get objects for controller
        mainController = ControlFactory.getInstance().getMainController();

        //Add item functionality
        ImageButton addItemBtn = (ImageButton) findViewById(R.id.addItem_btn);
        addItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainController.addItem(SPApp.this);
            }
        });


        ImageButton camButton = (ImageButton)findViewById(R.id.addItem_cam);
        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        ImageButton btrButton = (ImageButton)findViewById(R.id.addItem_bt);
        btrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBTReaderDialog();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            image=Bitmap.createScaledBitmap(image, 150, 150, false);
            Intent intent = new Intent(getApplicationContext(), ItemDetails.class);
            Bundle b = new Bundle();
            b.putString("PRODUCT_NAME", ""); //Your id
            b.putParcelable("PRODUCT_IMG", image);
            intent.putExtras(b);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spapp, menu);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //customAdapter.refreshData();

        //Notification set up
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());
        System.out.print("Hours: " + c.getTime().getHours());
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, NotificationService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        am.cancel(pi);


        // by my own convention, minutes <= 0 means notifications are disabled
        if (c.getTime().getHours() >= 14 ) {
            int minutes = 60*24*60;
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 30*1000,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
        }
        startService(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.action_watch_list){
            Intent intent =  new Intent(getApplicationContext(), ShopCreateActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.action_expiring_items){
            Intent intent =  new Intent(getApplicationContext(), ExpiringItems.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
    public void setActivityBackgroundColor(String color) {
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(Color.parseColor(color));
    }

    public void showBTReaderDialog() {
        final Dialog d = new Dialog(SPApp.this);
        d.setTitle("Barcode Reader Connected");
        d.setContentView(R.layout.btrdialog);
        final EditText barcode = (EditText)d.findViewById(R.id.barCodeText);
        barcode.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                        (i == KeyEvent.KEYCODE_ENTER)) {
                    getProductDetailsAndAdd(barcode.getText().toString(), d);
                    barcode.setEnabled(false);
                    return true;
                }
                return false;
            }
        });
        Button doneBtn = (Button)d.findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    public void getProductDetailsAndAdd(String code, Dialog dialog){
        final String barcode = code;
        final Dialog dlg = dialog;
        new AsyncTask<Void, Void, ArrayList<String>>(){
            Drawable d;
            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                ArrayList<String> details = null;
                try {
                    details = new ItemLookup(getApplicationContext()).GetProductDetails(barcode);
                    InputStream is = (InputStream)new URL(details.get(1)).getContent();
                    d = Drawable.createFromStream(is,null);
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (ItemNotFoundException e) {
                    details = null;
                }
                return details;
            }
            @Override
            protected void onPostExecute(ArrayList<String> s) {
                if (s==null){
                    addProduct(null, null, dlg);
                }else {
                    System.out.println("PRODUCT XXXXXXXXXXXX" + s.get(0));
                    addProduct(s.get(0), d, dlg);
                }
            }
        }.execute();

    }
    private void addProduct(String prodTitle,Drawable image, Dialog dlg) {
        if(prodTitle==null){
            Toast.makeText(SPApp.this, "Product Not Found.", Toast.LENGTH_SHORT).show();
            MediaPlayer player = MediaPlayer.create(getApplicationContext(),R.raw.beep);
            player.start();
        }else{
            Bitmap bitmap = ((BitmapDrawable)image).getBitmap();
            bitmap=Bitmap.createScaledBitmap(bitmap, 150,150,false);
            try {
                ControlFactory.getInstance().getItemController().addItem(getApplicationContext(), "MISC", prodTitle, bitmap, null, 1);
                Toast.makeText(SPApp.this, "Product Added", Toast.LENGTH_SHORT).show();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        final EditText barcode = (EditText)dlg.findViewById(R.id.barCodeText);
        barcode.setEnabled(true);
        barcode.setText("");
    }
    /*@Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info=
                (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
        int groupPos =ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int menuItemIndex = item.getItemId();
        //String[] menuItems = getResources().getStringArray(R.array.menu);
        //String menuItemName = menuItems[menuItemIndex];
        Product selProd = (Product)expListView.getExpandableListAdapter().getGroup(groupPos);
        boolean flag=true;

        switch (menuItemIndex)
        {
            case 0: AddItemDialog itmDialog = new AddItemDialog(this,selProd,customAdapter);
                itmDialog.show();
            default: flag=false;
        }
        return flag;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==android.R.id.list) {
            ExpandableListView.ExpandableListContextMenuInfo info=
                    (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;
            int groupPos =ExpandableListView.getPackedPositionGroup(info.packedPosition);
            Product selProd = (Product)expListView.getExpandableListAdapter().getGroup(groupPos);
            menu.setHeaderTitle(selProd.getProductName());
            String[] menuItems = getResources().getStringArray(R.array.menu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }*/
}
