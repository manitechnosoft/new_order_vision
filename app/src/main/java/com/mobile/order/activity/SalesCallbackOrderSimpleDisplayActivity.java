package com.mobile.order.activity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mobile.order.BaseApplication;
import com.mobile.order.R;
import com.mobile.order.adapter.FirestoreSalesCallback;
import com.mobile.order.adapter.SalesAdapterGroupByDate;
import com.mobile.order.helper.AppUtil;
import com.mobile.order.helper.FirestoreUtil;
import com.mobile.order.helper.FontHelper;
import com.mobile.order.helper.Fonts;
import com.mobile.order.model.Config;
import com.mobile.order.model.DaoSession;
import com.mobile.order.model.SalesFilter;
import com.mobile.order.model.SalesOrder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
public class SalesCallbackOrderSimpleDisplayActivity extends BaseActivity implements FirestoreSalesCallback {
	private Config config;
	@BindView(R.id.toolbar)
    Toolbar toolbar;
	@BindView(R.id.filter_bar)
	CardView filterBar;

	@BindView(R.id.filter_bar_container)
	FrameLayout filterBarContainer;

	@BindView(R.id.add_sales_order)
	FloatingActionButton floatingAddSalesOrder;

	//ListView listView;
	List<SalesOrder>salesNewList = new ArrayList<>();
	Map<String,List<SalesOrder>> salesMapByMonthAndYear = new HashMap<>();

	DaoSession daoSession;
	private SalesAdapterGroupByDate salesAdapterGroupByDate;
	FirestoreUtil util=new FirestoreUtil();
	private FilterSalesDialogFragment mFilterDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sales_display_with_filter);
		ButterKnife.bind(this);
		daoSession = ((BaseApplication) getApplication()).getDaoInstance();
		//listView = findViewById(R.id.list_view);
		mFilterDialog = new FilterSalesDialogFragment();
		SalesItemListener mItemListener = new SalesItemListener() {
			@Override
			public void onNoteClick(SalesOrder clickedSale) {
				//mActionsListener.openNoteDetails(clickedNote);
			}
		};
		setupListView();
		initializeToolbar();
		filterBarContainer.setVisibility(View.GONE);
		filterBar.setVisibility(View.GONE);
		config = ((BaseApplication) getApplication()).getConfig();
	}

	@Override
	protected void onResume() {
		super.onResume();

		fetchSalesList();
	}


	private void setupListView() {
		//// Load all items
		//salesNewList.addAll(salesDao.loadAll());
		AppCompatActivity activity=(AppCompatActivity)this;
		salesAdapterGroupByDate = new SalesAdapterGroupByDate(activity,salesMapByMonthAndYear, salesNewList, mItemListener);
		RecyclerView recyclerView = findViewById(R.id.sales_list_holder);
		//DividerItemDecoration dividerItemDecoration =new com.google.firebase.quickstart.auth.greendao.DividerItemDecoration(ContextCompat.getDrawable(getApplicationContext(), R.drawable.scissors_50));
		recyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_close)));
		//recyclerView.setHasFixedSize(true);
		//recyclerView.setLayoutManager(llm);
		int numColumns = 2;
		//recyclerView.setHasFixedSize(true);
		//recyclerView.setLayoutManager(new GridLayoutManager(this, numColumns));
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		recyclerView.setAdapter(salesAdapterGroupByDate);


		salesAdapterGroupByDate.notifyDataSetChanged();
	}

	public void fetchSalesList(){
		salesNewList.clear();
		//// Load all items
		//salesNewList.addAll(salesDao.loadAll());
		//util.getSales(this);
		//Fetch Only Today's records
		SalesFilter aFilter= AppUtil.getSalesFilters(null,null, null, null, null,0);
		util.getSalesWithFilter(this,aFilter,false);

	}
	public void loadSalesItems(List<SalesOrder>salesList) {
		final Calendar cldr = Calendar.getInstance();
		salesMapByMonthAndYear.clear();
		salesNewList.clear();
		for (SalesOrder aSale : salesList) {
			if(null!=aSale.getUpdatedOn()){
				cldr.setTime(aSale.getUpdatedOn());
				int month = cldr.get(Calendar.MONTH)+1;
				int year = cldr.get(Calendar.YEAR);
				String monthAndYear = month+"-"+year;
				//aSale.setMonAndYear(monthAndYear);
				if(salesMapByMonthAndYear.containsKey(monthAndYear))
				{
					List<SalesOrder>salesByMonthAndYearList = salesMapByMonthAndYear.get(monthAndYear);
					salesByMonthAndYearList.add(aSale);
				}
				else{
					List<SalesOrder>salesByMonthAndYearList = new ArrayList<>();
					salesByMonthAndYearList.add(aSale);
					salesMapByMonthAndYear.put(monthAndYear,salesByMonthAndYearList );
				}
			}
			salesNewList.add(aSale);
		}
		salesAdapterGroupByDate.notifyDataSetChanged();
	}

	public void addNewItem(View view) {
		// Go to add item activity
		Intent intent = new Intent(this,SalesOrderActivity.class);
		intent.putExtra("create",true);
		startActivity(intent);
	}

	SalesCallbackOrderDisplayActivity.SalesItemListener mItemListener = new SalesCallbackOrderDisplayActivity.SalesItemListener() {
		@Override
		public void onNoteClick(SalesOrder clickedSale) {
			//mActionsListener.openNoteDetails(clickedNote);
		}
	};

	public interface SalesItemListener {

		void onNoteClick(SalesOrder clickedSale);
	}
	@OnClick(R.id.filter_bar)
	public void onFilterClicked() {
		// Show the dialog containing filter options
//		FilterSalesDialogFragment fragment1 = FilterSalesDialogFragment.newInstance();
//		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
//		fragmentTransaction.replace(R.id.fragmentContainer, fragment1, FilterSalesDialogFragment.TAG);
//		fragmentTransaction.addToBackStack(FilterSalesDialogFragment.TAG);
//		fragmentTransaction.commit();
		mFilterDialog.show(getSupportFragmentManager(), FilterSalesDialogFragment.TAG);
		//mFilterDialog.addToBackStack(FilterSalesDialogFragment.TAG);
	}
	@OnClick(R.id.button_clear_filter)
	public void onClearFilterClicked() {
		mFilterDialog.resetFilters();

		//onFilter(Filters.getDefault());
	}
	public class DividerItemDecoration extends RecyclerView.ItemDecoration {

		private Drawable mDivider;

		public DividerItemDecoration(Drawable divider) {
			mDivider = divider;
		}

		@Override
		public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
			super.getItemOffsets(outRect, view, parent, state);
			int position = parent.getChildAdapterPosition(view);
			if (position== 0) {
				return;
			}
			int fivedp = getResources().getDimensionPixelOffset(R.dimen.tile_padding);
			outRect.top = mDivider.getIntrinsicHeight()-(7*fivedp);

			/*int totalWidth = parent.getWidth();
			int maxCardWidth = getResources().getDimensionPixelOffset(R.dimen.sales_activity_horizontal_margin);
			int sidePadding = (totalWidth - maxCardWidth) / 2;
			sidePadding = Math.max(0, sidePadding);
			outRect.set(0,maxCardWidth, 0, maxCardWidth);*/
		}

		@Override
		public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
			//int dividerLeft = parent.getPaddingLeft();
			int fivedp = getResources().getDimensionPixelOffset(R.dimen.tile_padding);
			int dividerLeft = parent.getWidth()/2-getResources().getDimensionPixelOffset(R.dimen.tile_padding);
			//int dividerRight = parent.getWidth() - parent.getPaddingRight();
			int dividerRight = parent.getWidth()/2+fivedp+fivedp;

			int childCount = parent.getChildCount();
			for (int i = 0; i < childCount - 1; i++) {
				View child = parent.getChildAt(i);

				RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
				int dividerTop = child.getBottom() + params.bottomMargin;
				int dividerBottom = dividerTop + mDivider.getIntrinsicHeight()-(7*fivedp);
				if(i+1<childCount){
					ConstraintLayout nextChild = (ConstraintLayout)parent.getChildAt(i+1);
					TextView monthYear = (TextView)nextChild.getChildAt(0);
					Paint paint = new Paint();
					paint.setTextSize(36f);
					paint.setColor(Color.BLACK);
					paint.setTextAlign(Paint.Align.LEFT);
					paint.setAntiAlias(true);
					String displayText = AppUtil.getShortMonthFromMonthAndYear(monthYear.getText().toString());
					canvas.drawText(displayText, fivedp,dividerBottom, paint);
				}

				mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
				mDivider.draw(canvas);
			}
		}
	}
	/**
	 * Method used to initialize toolbar
	 */
	private void initializeToolbar() {
		setSupportActionBar(toolbar);
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
			//supportActionBar.setDisplayShowHomeEnabled(true);
			supportActionBar.setTitle(AppUtil.applyFontStyle(
					this.getResources().getString(R.string.sales_orders_list),
					FontHelper.getFont(Fonts.MULI_SEMI_BOLD))
			);
			supportActionBar.setDisplayShowTitleEnabled(true);
			supportActionBar.setDisplayShowCustomEnabled(true);
			//supportActionBar.setCustomView(R.layout.custom_action_bar_layout);
			//supportActionBar.setElevation(4);
		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String user= AppUtil.getFromLoginPref(this);
			if (item.getItemId() == android.R.id.home) {
				if(user.equals("SALES")) {
					Intent intent = new Intent(this, SalesOrderLandActivity.class);
					this.startActivity(intent);
					finish();
					return true;
				}
				if(user.equals("ADMIN")) {
					Intent intent = new Intent(this, MainActivity.class);

					this.startActivity(intent);
					finish();
					return true;
				}
			}
		if (item.getItemId() == R.id.action_logout) {
			config.logoutUser(this);
		}
		if (item.getItemId() == R.id.refresh) {
			fetchSalesList();
		}
		return true;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}
}
