package com.example.ghetto.cart

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ghetto.Constants
import com.example.ghetto.R
import com.example.ghetto.databinding.FragmentCartBinding
import com.example.ghetto.entities.Order
import com.example.ghetto.entities.Product
import com.example.ghetto.entities.ProductOrder
import com.example.ghetto.order.OrderActivity
import com.example.ghetto.product.MainAux
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CartFragment : BottomSheetDialogFragment() , OnCartListener {
    private var binding : FragmentCartBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var adapter: ProductCartAdapter
    private var totalPrice = 0.0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentCartBinding.inflate(LayoutInflater.from(activity))
        binding?.let{
            val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
            bottomSheetDialog.setContentView(it.root)
            bottomSheetBehavior = BottomSheetBehavior.from(it.root.parent as View)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            setUpRecyclerView()
            configButtons()
            getProducts()

            return bottomSheetDialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun configButtons() {
        binding?.let{
            it.ibCancel.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            it.efab.setOnClickListener {
                requestOrder()
            }

        }
    }

    private fun requestOrder() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->
            enableUI(false)
            val products = hashMapOf<String, ProductOrder>()
            adapter.getProducts().forEach { product ->
                products.put(product.id!!,
                    ProductOrder(product.id!!, product.name!!, product.newQuantity)
                )
            }
            val order = Order(clientId = myUser.uid,
                products = products,
                totalPrice = totalPrice,
                status = 1 )

            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_REQUEST)
                .add(order)
                .addOnSuccessListener {
                    dismiss()
                    (activity as? MainAux)?.clearCart()
                    startActivity(Intent(context, OrderActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al comprar", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                }
        }

    }

    private fun enableUI(enable : Boolean){
        binding?.let{
            it.ibCancel.isEnabled = enable
            it.efab.isEnabled = enable
        }
    }

    private fun getProducts(){
        (activity as? MainAux)?.getProductsCart()?.forEach{
            adapter.add(it)
        }
    }

    private fun setUpRecyclerView() {
        binding?.let{
            adapter = ProductCartAdapter(mutableListOf(), this)

            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@CartFragment.adapter
            }
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.updateTotal()
        super.onDestroyView()
        binding = null
    }

    override fun setQuantity(product: Product) {
        adapter.update(product)

    }

    override fun showTotal(total: Double) {
        totalPrice = total
        binding?.let{
            it.tvTotal.text = getString(R.string.product_full_cart, total)
        }
    }
}