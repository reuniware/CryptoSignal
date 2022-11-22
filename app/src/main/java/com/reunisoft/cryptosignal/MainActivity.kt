package com.reunisoft.cryptosignal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request


var cryptoSignalViewModel = CryptoSignalViewModel()

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val coinAdapter = CoinsAdapter { coin -> adapterOnClick(coin)}
        val recyclerView: RecyclerView? = findViewById(R.id.recycler_view)
        recyclerView?.adapter = coinAdapter

        cryptoSignalViewModel.getCurrentListOfCoins().observe(this) {
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
    private var currentListOfCoins = MutableLiveData<ArrayList<Coin>>()

    fun getCurrentListOfCoins(): MutableLiveData<ArrayList<Coin>> {
        return currentListOfCoins
    }

    //https://api.binance.com/api/v1/exchangeInfo

    var isActive = true

    var listOfSymbols = ArrayList<String>()

    fun process() {

        CoroutineScope(Dispatchers.IO).launch {

            while(isActive) {
                val listOfCoins = ArrayList<Coin>()

//                if (listOfSymbols.size == 0) {
//                    listOfSymbols = request0()
//                }

                if (listOfSymbols.size == 0) {
                    listOfSymbols.addAll(listOf("BTCUSDT", "ETHUSDT", "LTCUSDT", "BNXUSDT", "TWTUSDT", "AUTOUSDT", "KEYUSDT", "DOCKUSDT", "DREPUSDT", "CELOUSDT", "QIUSDT", "LEVERUSDT"))
                }

                listOfSymbols.forEach {
                    try {
                        if (it.endsWith("USDT")) {
                            val symbol = it
                            val price = request(symbol)
                            listOfCoins.add(Coin(symbol.replace("USDT", "/USDT"), price))
                        }
                    } catch (_: java.lang.Exception){
                    }
                }

                currentListOfCoins.postValue(listOfCoins)

                //delay(500)
            }
        }
    }
}

fun request0(): ArrayList<String> {

    val listOfSymbols = ArrayList<String>()

    val client2 = OkHttpClient()
    val request2 = Request.Builder()
        .url("https://api.binance.com/api/v3/exchangeInfo")
        .build()
    client2.newCall(request2).execute().use {
        if (!it.isSuccessful) {
            Log.d("cryptosignalviewmodel", "erreur = ${it.code}")
        } else {
            Log.d("cryptosignalviewmodel", "Ok")
            val body = it.body!!.string()
            val gson = Gson()

            val typeToken = object : TypeToken<Any>() {}.type
            val exchangeInfo = gson.fromJson<Any>(body, typeToken)

            Log.d("exchangeInfo", exchangeInfo.toString())
            val a = (exchangeInfo as com.google.gson.internal.LinkedTreeMap<*, *>).entries.toTypedArray()[4]
            Log.d("exchangeInfo", a.toString())
            val b = a.value
            Log.d("exchangeInfo b.toString", b.toString())

            val symbolInfo = gson.fromJson<Any>(b.toString(), typeToken)
            Log.d("symbolInfo", symbolInfo.toString())

            (symbolInfo as ArrayList<*>).forEach {
                val symbol = (it as com.google.gson.internal.LinkedTreeMap<*, *>).entries.toTypedArray()[0]
                Log.d("symbolInfo", symbol.value as String)
                listOfSymbols.add(symbol.value as String)
            }
        }
    }
    return listOfSymbols
}

fun request(symbol: String): String {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=1m&limit=1")
        .build()
    client.newCall(request).execute().use {
        if (!it.isSuccessful) {
            Log.d("cryptosignalviewmodel", "erreur = ${it.code}")
        } else {
            Log.d("cryptosignalviewmodel", "Ok")

            val body = it.body!!.string()
            val gson = Gson()

            val typeToken = object : TypeToken<Any>() {}.type
            val coins = gson.fromJson<Any>(body, typeToken)

            val coin = coins as ArrayList<*>
            val coin0 = coin[0]
            val open = (coin0 as java.util.ArrayList<*>)[1]
            val high = (coin0 as java.util.ArrayList<*>)[2]
            val low = (coin0 as java.util.ArrayList<*>)[3]
            val close = (coin0 as java.util.ArrayList<*>)[4]

            Log.d("open",  open.toString())
            Log.d("high",  high.toString())
            Log.d("low",  low.toString())
            Log.d("close",  close.toString())

            return close.toString()
        }
    }
    return ""
}

class Coin(symbol_: String, price_: String) {
    val symbol : String = symbol_
    val price : String = price_
}

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
    private val coinSymbol: TextView = itemView.findViewById(R.id.coin_symbol)
    private val coinPrice: TextView = itemView.findViewById(R.id.coin_price)
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
        coinSymbol.text = coin.symbol
        coinPrice.text = coin.price
    }
}

object CoinDiffCallback : DiffUtil.ItemCallback<Coin>() {
    override fun areItemsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        return oldItem.symbol == newItem.symbol
    }
}