package com.reunisoft.cryptosignal

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path


var cryptoSignalViewModel = CryptoSignalViewModel()

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val coinAdapter = CoinsAdapter { coin -> adapterOnClick(coin)}
        val recyclerView: RecyclerView? = findViewById(R.id.recycler_view)
        recyclerView?.adapter = coinAdapter

        cryptoSignalViewModel.getCurrentList().observe(this) {
            Log.d("cryptoSignalViewModel", "will update displayed data")
            coinAdapter.submitList(it)
        }

        cryptoSignalViewModel.process()

    }

    private fun adapterOnClick(coin: Coin) {
//        val intent = Intent(this, InformationDetailActivity()::class.java)
//        intent.putExtra(INFORMATION_ID, information.id)
//        startActivity(intent)
    }
}


class CryptoSignalViewModel : ViewModel() {
    //private val BASE_URL = "https://www.google.com"
    //private val retrofit = Retrofit.Builder().baseUrl(BASE_URL).build()
    private var currentList = MutableLiveData<List<Coin>>()

    fun getCurrentList(): MutableLiveData<List<Coin>> {
        return currentList
    }

    fun process() {

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.coingecko.com/api/v3/coins/list")
                .build()
            client.newCall(request).execute().use {
                if (!it.isSuccessful) {
                    Log.d("cryptosignalviewmodel", "erreur = ${it.code}")
                } else {
                    Log.d("cryptosignalviewmodel", "Ok")
                    val body = it.body!!.string()
                    val gson = Gson()

                    val typeToken = object : TypeToken<List<Coin>>() {}.type
                    val coins = gson.fromJson<List<Coin>>(body, typeToken)

//                    coins.forEach {  c ->
//                        Log.d("coin", "${c.id} ${c.name} ${c.symbol}")
//                    }

                    //currentList = MutableLiveData(coins)

                    currentList.postValue(coins)
                }
            }
        }
    }
}

class Coin(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("symbol")
    val symbol: String = "",
    @SerializedName("name")
    val name: String = ""
)


class CoinsAdapter(private val onClick: (Coin) -> Unit) : ListAdapter<Coin, CoinViewHolder>(CoinDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coin, parent, false)
        return CoinViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val information = getItem(position)
        holder.bind(information)
    }
}

class CoinViewHolder(itemView: View, val onClick: (Coin) -> Unit) : RecyclerView.ViewHolder(itemView) {
    private val coinIdView: TextView = itemView.findViewById(R.id.coin_id)
    private val coinNameView: TextView = itemView.findViewById(R.id.coin_name)
    private val coinSymbolView: TextView = itemView.findViewById(R.id.coin_symbol)
    private var currentCoin: Coin? = null

    init {
        itemView.setOnClickListener {
            currentCoin?.let {
                onClick(it)
            }
        }
    }

    fun bind(coin: Coin) {
        currentCoin = coin
        coinIdView.text = coin.id
        coinNameView.text = coin.name
        coinSymbolView.text = coin.symbol
    }
}

object CoinDiffCallback : DiffUtil.ItemCallback<Coin>() {
    override fun areItemsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        return oldItem.id == newItem.id
    }
}