package sg.edu.nus.iss.smartpantry.controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.text.ParseException;
import java.util.Date;
import java.util.Random;

import sg.edu.nus.iss.smartpantry.Entity.Item;
import sg.edu.nus.iss.smartpantry.Entity.Product;
import sg.edu.nus.iss.smartpantry.application.util.ScanBarcodeActivity;
import sg.edu.nus.iss.smartpantry.dao.CategoryDao;
import sg.edu.nus.iss.smartpantry.dao.ItemDao;
import sg.edu.nus.iss.smartpantry.dao.ProductDao;

/**
 * Created by A0134493A on 7/5/2015.
 */
public class ItemController {

    public Intent showAddItem(Context context){
        Intent intent = new Intent(context,ScanBarcodeActivity.class);
        return intent;
    }

    public void addItem(Context context,  String categoryName,String productName, Bitmap bitmap, Date expiryDate, int thresholdQty) throws ParseException {
        ItemDao itemDao= DAOFactory.getItemDao(context);
        CategoryDao catDao = DAOFactory.getCategoryDao(context);
        ProductDao prodDao = DAOFactory.getProductDao(context);
        boolean isExistingProduct = prodDao.isProductExists(categoryName,productName);
        Random rand = new Random();
        if(isExistingProduct == false)
        {
            Product product = new Product(categoryName,productName);
            product.setThreshold(thresholdQty);
            product.setProdImage(bitmap);
            prodDao.addProduct(product);
        }

        int itemId = itemDao.generateItemIdForProduct(productName);
        if(itemId != -1)
        {
                Item itm = new Item(categoryName,productName, itemId);
                if (expiryDate!=null)
                    itm.setExpiryDate(new java.sql.Date(expiryDate.getTime()));
                itemDao.addItem(itm);
        }
        else {
            Toast.makeText(context, "Error in generating the Item ID!!!!", Toast.LENGTH_SHORT).show();
        }
    }
}
