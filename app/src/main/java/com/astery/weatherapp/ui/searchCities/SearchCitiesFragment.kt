package com.astery.weatherapp.ui.searchCities

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.astery.weatherapp.app.di.appComponent
import com.astery.weatherapp.databinding.SearchCitiesFragmentBinding
import com.astery.weatherapp.model.pogo.WeatherData
import com.astery.weatherapp.model.state.*
import com.astery.weatherapp.ui.BaseFragment
import com.astery.weatherapp.ui.BindingInflater
import com.astery.weatherapp.ui.favCities.FavCitiesAdapter
import com.astery.weatherapp.ui.loadState.LoadStateView
import javax.inject.Inject

class SearchCitiesFragment : BaseFragment<SearchCitiesFragmentBinding>() {
    private val viewModel: SearchCitiesViewModel by lazy {
        factory.create()
    }

    @Inject
    lateinit var factory: SearchCitiesViewModel.Factory

    private val adapter: FavCitiesAdapter by lazy(mode = LazyThreadSafetyMode.NONE) {
        // it assumes that adapter will be called only if the viewModel returns list
        FavCitiesAdapter(
            (viewModel.cities.value!! as Completed<List<WeatherData>>).result,
            this::moveToWeather
        )
    }


    override fun inflateBinding(): BindingInflater<SearchCitiesFragmentBinding> {
        return SearchCitiesFragmentBinding::inflate
    }

    override fun inject() {
        context?.appComponent?.inject(this)
    }

    override fun setViewModelListeners() {
        viewModel.cities.observe(viewLifecycleOwner, FavCitiesObserver())
    }

    override fun prepareUI() {
        bind.back.setOnClickListener { activity?.onBackPressed() }
        bind.searchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                bind.search.performClick()
                false
            } else false
        }


        bind.search.setOnClickListener {
            hideSearchKeyboard()
            viewModel.getCities(bind.searchText.text.toString())
        }
    }

    private fun moveToWeather(city: WeatherData) {
        findNavController().navigate(
            SearchCitiesFragmentDirections.actionSearchCitiesFragmentToWeatherTodayFragment(
                city
            )
        )
    }


    private fun hideSearchKeyboard() {
        val imm =
            activity?.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(bind.searchText.windowToken, 0)
    }

    // MARK: render
    private inner class FavCitiesObserver : Observer<Result<List<WeatherData>>> {
        override fun onChanged(result: Result<List<WeatherData>>?) {
            when (result) {
                is Idle -> renderLoading()
                is Loading -> renderLoading()
                is Completed<*> -> renderComplete(result.result as List<WeatherData>)
                is GotNothing -> renderNothing()
                else -> renderNothing()
            }
        }

        private fun renderNothing() {
            bind.loadStateView.changeState(LoadStateView.StateNothing(), bind.recyclerView)
        }

        private fun renderLoading() {
            bind.loadStateView.changeState(LoadStateView.StateLoading(), bind.recyclerView)
        }

        private fun renderComplete(weather: List<WeatherData>) {
            bind.loadStateView.changeState(LoadStateView.StateHide(), bind.recyclerView)

            bind.run {
                recyclerView.layoutManager =
                    LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
                adapter.submitList(weather)
                recyclerView.adapter = adapter
            }
        }

    }

}