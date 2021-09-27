package com.example.ghetto.cart

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ghetto.R
import com.example.ghetto.databinding.ItemProductCartBinding
import com.example.ghetto.entities.Product

class ProductCartAdapter(private val productList : MutableList<Product>,
                         private val listener: OnCartListener) : RecyclerView.Adapter<ProductCartAdapter.ViewHolder>(){
    private lateinit var context : Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val binding = ItemProductCartBinding.bind(view)

        fun setListener(product: Product){
            binding.ibRes.setOnClickListener{
                if (product.newQuantity > 1){
                    product.newQuantity -=1
                    listener.setQuantity(product)
                }
            }

            binding.ibSum.setOnClickListener{
                if (product.newQuantity < product.quantity){
                    product.newQuantity +=1
                    listener.setQuantity(product)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_product_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        holder.setListener(product)
        holder.binding.tvName.text = product.name
        holder.binding.tvQuantity.text = product.newQuantity.toString()
        Glide.with(context)
            .load(product.imgUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_baseline_error_24)
            .centerCrop()
            .circleCrop()
            .into(holder.binding.imgProduct)

    }

    fun getProducts() : List<Product> = productList

    fun add(product: Product){
        if (!productList.contains(product)){
            productList.add(product)
            notifyItemInserted(productList.size - 1)
            calcTotal()
        } else {
            update(product)
        }
    }

    fun update(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.set(index, product)
            notifyItemChanged(index)
            calcTotal()
        }
    }

    fun delete(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
            calcTotal()
        }
    }

    private fun calcTotal(){
        var result = 0.0
        for (product in productList)
            result += product.totalPrice()
        listener.showTotal(result)
    }

    override fun getItemCount(): Int = productList.size

}