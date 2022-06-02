package com.app.programacion.activities;

import static com.app.programacion.utils.Constant.BANNER_CATEGORY_DETAIL;
import static com.app.programacion.utils.Constant.FILTER_SHOW_ALL_RECIPES;
import static com.app.programacion.utils.Constant.FILTER_SHOW_ONLY_RECIPES_POSTS;
import static com.app.programacion.utils.Constant.FILTER_SHOW_ONLY_RECIPES_VIDEOS;
import static com.app.programacion.utils.Constant.INTERSTITIAL_ON_RECIPES_LIST;
import static com.app.programacion.utils.Constant.RECIPES_GRID_2_COLUMN;
import static com.app.programacion.utils.Constant.RECIPES_GRID_3_COLUMN;
import static com.app.programacion.utils.Constant.RECIPES_LIST_BIG;
import static com.app.programacion.utils.Constant.RECIPES_LIST_SMALL;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.programacion.R;
import com.app.programacion.adapters.AdapterRecipes;
import com.app.programacion.callbacks.CallbackCategoryDetail;
import com.app.programacion.config.AppConfig;
import com.app.programacion.databases.prefs.AdsPref;
import com.app.programacion.databases.prefs.SharedPref;
import com.app.programacion.models.Category;
import com.app.programacion.models.Recipe;
import com.app.programacion.rests.ApiInterface;
import com.app.programacion.rests.RestAdapter;
import com.app.programacion.utils.AdsManager;
import com.app.programacion.utils.AppBarLayoutBehavior;
import com.app.programacion.utils.Constant;
import com.app.programacion.utils.ItemOffsetDecoration;
import com.app.programacion.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCategoryDetail extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterRecipes adapterRecipes;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackCategoryDetail> callbackCall = null;
    private int post_total = 0;
    private int failed_page = 0;
    private Category category;
    SharedPref sharedPref;
    private ShimmerFrameLayout lyt_shimmer;
    AdsPref adsPref;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_recipe_list);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        sharedPref.setDefaultFilterRecipes(0);
        adsPref = new AdsPref(this);

        AppBarLayout appBarLayout = findViewById(R.id.appbarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_CATEGORY_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_ON_RECIPES_LIST, adsPref.getInterstitialAdInterval());

        category = (Category) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = findViewById(R.id.recyclerView);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.grid_space_recipes);
        recyclerView.addItemDecoration(itemDecoration);
        if (sharedPref.getRecipesViewType() == RECIPES_LIST_SMALL || sharedPref.getRecipesViewType() == RECIPES_LIST_BIG) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getRecipesViewType() == RECIPES_GRID_3_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getRecipesViewType() == RECIPES_GRID_2_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        }

        //set data and list adapter
        adapterRecipes = new AdapterRecipes(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterRecipes);

        // on item list clicked
        adapterRecipes.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityRecipeDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
        });

        // detect when scroll reach bottom
        adapterRecipes.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) {
                callbackCall.cancel();
            }
            adapterRecipes.resetListData();
            requestAction(1);
        });

        requestAction(1);
        initShimmerLayout();
        setupToolbar();

    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterRecipes.getItemCount() - current_page);
        if (post_total > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterRecipes.setLoaded();
        }
    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(category.category_name);
        }
    }

    private void displayApiResult(final List<Recipe> recipes) {
        adapterRecipes.insertDataWithNativeAd(recipes);
        swipeProgress(false);
        if (recipes.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());

        if (sharedPref.getCurrentFilterRecipes() == 0) {
            callbackCall = apiInterface.getRecipesByCategory(category.cid, page_no, AppConfig.POST_PER_PAGE, FILTER_SHOW_ALL_RECIPES, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentFilterRecipes() == 1) {
            callbackCall = apiInterface.getRecipesByCategory(category.cid, page_no, AppConfig.POST_PER_PAGE, FILTER_SHOW_ONLY_RECIPES_POSTS, AppConfig.REST_API_KEY);
        } else if (sharedPref.getCurrentFilterRecipes() == 2) {
            callbackCall = apiInterface.getRecipesByCategory(category.cid, page_no, AppConfig.POST_PER_PAGE, FILTER_SHOW_ONLY_RECIPES_VIDEOS, AppConfig.REST_API_KEY);
        } else {
            callbackCall = apiInterface.getRecipesByCategory(category.cid, page_no, AppConfig.POST_PER_PAGE, FILTER_SHOW_ALL_RECIPES, AppConfig.REST_API_KEY);
        }

        callbackCall.enqueue(new Callback<CallbackCategoryDetail>() {
            @Override
            public void onResponse(Call<CallbackCategoryDetail> call, Response<CallbackCategoryDetail> response) {
                CallbackCategoryDetail resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.posts);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackCategoryDetail> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        adapterRecipes.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getApplicationContext())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterRecipes.setLoading();
        }
        new Handler().postDelayed(() -> requestPostApi(page_no), Constant.DELAY_TIME);
    }

    private void showFailedView(boolean show, String message) {
        View view = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            view.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view1 -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View view = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            view.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        });
    }

    private void initShimmerLayout() {
        View lyt_shimmer_recipes_list_small = findViewById(R.id.lyt_shimmer_recipes_list_small);
        View lyt_shimmer_recipes_list_big = findViewById(R.id.lyt_shimmer_recipes_list_big);
        View lyt_shimmer_recipes_grid2 = findViewById(R.id.lyt_shimmer_recipes_grid2);
        View lyt_shimmer_recipes_grid3 = findViewById(R.id.lyt_shimmer_recipes_grid3);
        if (sharedPref.getRecipesViewType() == RECIPES_LIST_SMALL) {
            lyt_shimmer_recipes_list_small.setVisibility(View.VISIBLE);
            lyt_shimmer_recipes_list_big.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid2.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getRecipesViewType() == RECIPES_LIST_BIG) {
            lyt_shimmer_recipes_list_small.setVisibility(View.GONE);
            lyt_shimmer_recipes_list_big.setVisibility(View.VISIBLE);
            lyt_shimmer_recipes_grid2.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getRecipesViewType() == RECIPES_GRID_2_COLUMN) {
            lyt_shimmer_recipes_list_small.setVisibility(View.GONE);
            lyt_shimmer_recipes_list_big.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid2.setVisibility(View.VISIBLE);
            lyt_shimmer_recipes_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getRecipesViewType() == RECIPES_GRID_3_COLUMN) {
            lyt_shimmer_recipes_list_small.setVisibility(View.GONE);
            lyt_shimmer_recipes_list_big.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid2.setVisibility(View.GONE);
            lyt_shimmer_recipes_grid3.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lyt_shimmer.stopShimmer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_search:
                Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                startActivity(intent);
                return true;

            case R.id.menu_filter:
                String[] items = getResources().getStringArray(R.array.dialog_filter_list);
                int itemSelected = sharedPref.getCurrentFilterRecipes();
                new AlertDialog.Builder(ActivityCategoryDetail.this)
                        .setTitle(R.string.dialog_title_filter)
                        .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                            if (callbackCall != null && callbackCall.isExecuted())
                                callbackCall.cancel();
                            adapterRecipes.resetListData();
                            requestAction(1);
                            sharedPref.updateFilterRecipes(position);
                            dialogInterface.dismiss();
                        })
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



}
