package com.example.ghetto.product

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ghetto.Constants
import com.example.ghetto.R
import com.example.ghetto.cart.CartFragment
import com.example.ghetto.databinding.ActivityMainBinding
import com.example.ghetto.detail.DetailFragment
import com.example.ghetto.entities.Product
import com.example.ghetto.order.OrderActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() ,OnProductListener ,MainAux  {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var binding : ActivityMainBinding
    private lateinit var adapter: ProductAdapter
    private lateinit var firestoreListener : ListenerRegistration

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var productSelected : Product? = null
    val productCartList = mutableListOf<Product>()


    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val response = IdpResponse.fromResultIntent(it.data)

        if(it.resultCode == RESULT_OK){
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null){
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()

                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                val token = preferences.getString(Constants.PROP_TOKEN, null)

                token?.let{
                    val db = FirebaseFirestore.getInstance()
                    val tokenMap = hashMapOf(Pair(Constants.PROP_TOKEN, token))

                    db.collection(Constants.COLL_USERS)
                        .document(user.uid)
                        .collection(Constants.COLL_TOKENS)
                        .add(tokenMap)
                        .addOnSuccessListener {
                            Log.i("registered token", token)
                            preferences.edit {
                                putString(Constants.PROP_TOKEN, null)
                                    .apply()
                            }
                        }
                        .addOnFailureListener {
                            Log.i("not registered token", token)
                        }
                }
            }else {
                if(response==null){
                    Toast.makeText(this, "Adios", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    response.error?.let{
                        if (it.errorCode == ErrorCodes.NO_NETWORK){
                            Toast.makeText(this, "Sin red", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this, "Codigo de Error: ${it.errorCode}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }

    override fun showButton(isVisible: Boolean) {
        binding.btnViewCart.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun onClick(product: Product) {
        val index = productCartList.indexOf(product)
        if (index != -1){
            productSelected = productCartList[index]
        } else {
            productSelected = product
        }
        val fragment = DetailFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

        showButton(false)

        //Analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
            param(FirebaseAnalytics.Param.ITEM_ID, product.id!!)
            param(FirebaseAnalytics.Param.ITEM_NAME, product.name!!)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configAuth()
        configRecyclerView()
        configButtons()
        configAnalytics()

        //FCM
        /*FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful){
                val token = task.result
                Log.i("get token", token.toString())
            } else {
                Log.i("not get token", task.exception.toString())
            }
        }*/

    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun configAuth(){
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null){
                supportActionBar?.title = auth.currentUser?.displayName
                binding.linearLayoutProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
            }else{
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build())

                resultLauncher.launch(
                    AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setIsSmartLockEnabled(false)
                    .build())
            }
        }

    }

    private fun configFirestoreRealtime(){
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)
        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null){
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges){
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id
                when(snapshot.type){
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete(product)
                }
            }
        }
    }



    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2,
                GridLayoutManager.VERTICAL,
                false)
            adapter = this@MainActivity.adapter
        }

    }

    private fun configButtons(){
        binding.btnViewCart.setOnClickListener{
            val fragment = CartFragment()
            fragment.show(supportFragmentManager.beginTransaction(), CartFragment::class.java.simpleName)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_sign_out ->{
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sesion Terminada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener{
                        if (it.isSuccessful){
                            binding.nsvProducts.visibility = View.GONE
                            binding.linearLayoutProgress.visibility = View.VISIBLE
                        }else{
                            Toast.makeText(this, "No se pudo cerrar la sesión", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            R.id.action_order_history ->{
                startActivity(Intent(this, OrderActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getProductsCart(): MutableList<Product> = productCartList

    override fun addProductToCart(product: Product) {
        val index = productCartList.indexOf(product)
        if (index != -1){
            productCartList.set(index, product)
        } else {
            productCartList.add(product)
        }
        updateTotal()
    }

    override fun clearCart() {
        productCartList.clear()
    }

    override fun getProductSelected(): Product? = productSelected

    override fun updateTotal() {
        var total = 0.0
        productCartList.forEach{
            total += it.totalPrice()
        }

        if (total ==0.0){
            binding.tvTotal.text = getString(R.string.product_empty_cart)
        }else{
            binding.tvTotal.text = getString(R.string.product_full_cart, total)

        }
    }
}