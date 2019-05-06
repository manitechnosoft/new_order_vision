package com.mobile.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobile.order.BaseApplication;
import com.mobile.order.R;
import com.mobile.order.adapter.FirestoreProductCallback;
import com.mobile.order.async.RefreshProduct;
import com.mobile.order.filter.MoneyValueFilter;
import com.mobile.order.helper.AppUtil;
import com.mobile.order.helper.FirestoreUtil;
import com.mobile.order.model.DaoSession;
import com.mobile.order.model.Product;
import com.mobile.order.model.ProductDao;

import org.w3c.dom.Text;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProductActivity extends BaseActivity implements
        FirestoreProductCallback {
    private Query mQuery;
    private ActionBar supportActionBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.product_doc_id)
    TextView productDocId;
    @BindView(R.id.cancel_product)
    Button cancel;

    @BindView(R.id.add_product)
    Button addProduct;

    @BindView(R.id.retail_type)
    Spinner retailType;

    @BindView(R.id.retail_price)
    EditText retailPrice;
    @BindView(R.id.product_name)
    EditText productField;
    @BindView(R.id.product_id)
    EditText productId;

    DaoSession daoSession;
    ProductDao productDao;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        ButterKnife.bind(this);
        initToolBar();
        daoSession = ((BaseApplication) getApplication()).getDaoInstance();
        productDao = daoSession.getProductDao();
        retailPrice.setKeyListener(new MoneyValueFilter());

        Bundle bundle = getIntent().getExtras();
        if(null!=bundle){
            Product aProduct = (Product) getIntent().getSerializableExtra("aProduct");
            productId.setText(aProduct.getProductId());
            productField.setText(aProduct.getProductName());
            retailPrice.setText(aProduct.getRetailSalePrice().toString());
            productDocId.setText(aProduct.getProductDocId());
            addProduct.setText(R.string.update_product);
        }

    }

    private void initToolBar(){
            setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
            supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                enableBackButton();
                supportActionBar.setDisplayShowTitleEnabled(false);
                supportActionBar.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.back_arrow_dark));
                supportActionBar.setDisplayShowTitleEnabled(true);
            }
    }
    /**
     * Method used to enable back button
     */
    private void enableBackButton() {
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setDisplayShowHomeEnabled(true);
    }
    @OnClick(R.id.add_product)
    public void addProduct(View button) {
        addProduct.setEnabled(false);
        Product aProduct = prepareProduct();
        validateProduct(aProduct);

        aProduct.setRetailSaleType(retailType.getSelectedItem().toString());
        Double price = Double.valueOf(retailPrice.getText().toString().isEmpty()?"0":retailPrice.getText().toString());
        aProduct.setRetailSalePrice(price);
if(!productDocId.getText().toString().isEmpty()){
    aProduct.setProductDocId(productDocId.getText().toString());
}
        updateProduct(aProduct);
    }
    @OnClick(R.id.cancel_product)
    public void cancel(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
    }
private Product prepareProduct(){
    String product = productField.getText().toString();
    Product aProduct = new Product();
    aProduct.setProductId(productId.getText().toString());
    aProduct.setProductName(product);
    return aProduct;
}



    public void updateProduct(final Product aProduct) {
        final String TAG = "updatePerson";
        if(validateProduct(aProduct)){
        Query query = FirestoreUtil.getProductCollectionRef();
        query = query.whereEqualTo("productId", aProduct.getProductId());

         query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task){
                if (task.isSuccessful()) {
                    if(task.getResult().size()==0){
                        FirestoreUtil.updateProductInFirebase(ProductActivity.this, aProduct);
                    }
                    else{
                        for (DocumentSnapshot document : task.getResult()) {
                            Product dbProduct = document.toObject(Product.class);
                            if(null!=aProduct.getProductDocId() && !document.getId().equals(aProduct.getProductDocId()))
                            {
                                Toast.makeText(ProductActivity.this, "Product Existed with the given product Id "+ aProduct.getProductId()+ "!",
                                        Toast.LENGTH_SHORT).show();
                                addProduct.setEnabled(true);
                                productId.requestFocusFromTouch();
                                productId.requestFocus();
                            }
                            else{
                                final DocumentReference productDocRef = FirestoreUtil.getProductCollectionRef().document(aProduct.getProductDocId());
                                productDocRef.set(aProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Product "+ aProduct.getProductName() +" has been updated Successfully!");
                                        Toast.makeText(getApplicationContext(),
                                                "Product "+ aProduct.getProductName() +" has been updated Successfully!",
                                                Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Product " + aProduct.getProductName() +" has not been updated!", e);
                                        Toast.makeText(getApplicationContext(),
                                                "Product "+ aProduct.getProductName() +" has not been updated!",
                                                Toast.LENGTH_LONG).show();
                                        addProduct.setEnabled(true);
                                        productId.requestFocusFromTouch();
                                        productId.requestFocus();
                                    }
                                });
                            }
                        }
                    }

                }
                else {
                    Toast.makeText(ProductActivity.this, "Error getting documents.",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            }
        });

        }
    }

    public void loadProducts(List<Product> productList){
        productDao.deleteAll();
        for(Product aProduct:productList){
            aProduct.setId(null);
            productDao.insert(aProduct);
        }
    }



    private boolean validateProduct(final Product aProduct){
        final String TAG = "updatePerson";
        String validationMessage = "";
        boolean validFlg = true;
        if(TextUtils.isEmpty(aProduct.getProductName()))
        {
            final TextView productErr = findViewById(R.id.productname_error_msg);
            validationMessage = "Product Name is required";
            productErr.setText(validationMessage);
            productErr.setVisibility(View.VISIBLE);
            validFlg = false;
        }
        if(TextUtils.isEmpty(aProduct.getProductId()))
        {
            validationMessage = validationMessage + "\nProduct Name is required";
            validFlg = false;
        }
        if(!validFlg){
            addProduct.setEnabled(true);
            Toast.makeText(getBaseContext(), validationMessage, Toast.LENGTH_SHORT).show();
        }
        return validFlg;
    }
}
