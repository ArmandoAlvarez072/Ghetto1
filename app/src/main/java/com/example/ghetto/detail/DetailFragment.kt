package com.example.ghetto.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ghetto.R
import com.example.ghetto.databinding.FragmentDetailBinding
import com.example.ghetto.entities.Product
import com.example.ghetto.product.MainAux

class DetailFragment : Fragment() {
    private var binding : FragmentDetailBinding? = null
    private var product : Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        binding?.let{
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProduct()
        setUpButtons()
    }


    private fun getProduct() {
        product = (activity as? MainAux)?.getProductSelected()
        product?.let{ product ->
            binding?.let{
                it.tvName.text = product.name
                it.tvDescription.text = product.description
                it.tvQuantity.text = getString(R.string.detail_quantity, product.quantity)
                setNewQuantity(product)
                Glide.with(this)
                    .load(product.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_baseline_error_24)
                    .centerCrop()
                    .into(it.imgProduct)

            }
        }
    }

    private fun setNewQuantity(product: Product) {
        binding?.let{
            val newQuantityStr = getString(R.string.detail_total_price,
                product.totalPrice(),
                product.newQuantity,
                product.price)
            it.etNewQuantity.setText(product.newQuantity.toString())
            it.tvTotalPrice.text = HtmlCompat.fromHtml(newQuantityStr,  HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }


    private fun setUpButtons() {
        product?.let { product ->
            binding?.let{ binding ->
                binding.ibRes.setOnClickListener{
                    if (product.newQuantity > 1){
                        product.newQuantity -=1
                        setNewQuantity(product)
                    }
                }
                binding.ibSum.setOnClickListener{
                    if (product.newQuantity < product.quantity){
                        product.newQuantity +=1
                        setNewQuantity(product)
                    }
                }
                binding.efab.setOnClickListener {
                    product.newQuantity = binding.etNewQuantity.text.toString().toInt()
                    addToCart(product)
                }
            }
        }
    }

    private fun addToCart(product: Product) {
        (activity as? MainAux)?.let{
            it.addProductToCart(product)
            activity?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }
}