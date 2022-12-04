package com.jumadi.pinview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Handler
import android.text.*
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager


/**
 * Created by Jumadi Janjaya date on 20/12/2021.
 * Jakarta, Indonesia.
 * Copyright (c) First Payment Indonesia. All rights reserved.
 **/
class PinView : View {

    var maxPIN = 6
    var itemSize = 25
    var itemMargin = 5
    var isKeyboardDefault = true
    var isPinVisible = false

    var pinVisibleDuration = -1
    private var mText = SpannableStringBuilder()
    private var chars = ArrayList<ItemText>()

    val text: Spannable
    get() = mText

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFakeBoldText = true

    }

    private var paintDebug = Paint(Paint.ANTI_ALIAS_FLAG)

    private var itemDrawable: Drawable? = ShapeDrawable(OvalShape()).apply {
        this.paint.color = Color.WHITE
        this.paint.style = Paint.Style.STROKE
        this.paint.strokeWidth = 2f
        this.paint.isAntiAlias = true
        this.paint.flags = Paint.ANTI_ALIAS_FLAG
    }

    private var itemFocusDrawable: Drawable? = ShapeDrawable(OvalShape()).apply {
        this.paint.color = Color.WHITE
        this.paint.style = Paint.Style.FILL
        this.paint.isAntiAlias = true
        this.paint.flags = Paint.ANTI_ALIAS_FLAG
    }

    private var a: TypedArray? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        a = context.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0)
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        a = context.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0)
        initialize()
    }

    private fun initialize() {
        try {
            a?.let {
                maxPIN = it.getInt(R.styleable.PinView_pinLength, 6)
                itemSize =  it.getDimensionPixelSize(R.styleable.PinView_pinItemSize, 12)
                itemMargin =  it.getDimensionPixelSize(R.styleable.PinView_pinItemMargin, 4)
                isKeyboardDefault =  it.getBoolean(R.styleable.PinView_keyboardDefault, true)
                isPinVisible =  it.getBoolean(R.styleable.PinView_pinVisible, false)
                pinVisibleDuration = it.getInt(R.styleable.PinView_pinVisibleDuration, -1)

                textPaint.textSize = it.getDimension(R.styleable.PinView_pinTextSize, (itemSize * 2).toFloat())
                textPaint.density = resources.displayMetrics.density
                textPaint.color = it.getColor(R.styleable.PinView_pinTextColor, Color.BLACK)

                if (it.hasValue(R.styleable.PinView_pinDrawable)) {
                    itemDrawable = it.getDrawable(R.styleable.PinView_pinDrawable)
                }

                if (it.hasValue(R.styleable.PinView_pinDrawableFocus)) {
                    itemFocusDrawable = it.getDrawable(R.styleable.PinView_pinDrawableFocus)
                }


                if (it.hasValue(R.styleable.PinView_pinDrawableTint)) {
                    itemDrawable?.setTint(it.getColor(R.styleable.PinView_pinDrawableTint, Color.BLACK))
                }

                if (it.hasValue(R.styleable.PinView_pinDrawableFocusTint)) {
                    itemFocusDrawable?.setTint(it.getColor(R.styleable.PinView_pinDrawableFocusTint, Color.BLACK))
                }

                it.recycle()
                init()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun init() {
        if (isKeyboardDefault) {
            requestFocus()
            isFocusableInTouchMode = true
            setOnKeyListener(OnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.unicodeChar == 0) { // control character
                        if (keyCode == KeyEvent.KEYCODE_DEL) {
                            removeChar()
                            Log.i("TAG", "text: $mText (keycode)")
                            invalidate()
                            return@OnKeyListener true
                        }
                    } else { // text character
                        addPinChar(event.unicodeChar.toChar())
                        Log.i("TAG", "text: $mText (keycode)")
                        invalidate()
                        return@OnKeyListener true
                    }
                }
                false
            })
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val itemSize = itemSize + itemMargin

        val newWid = (itemSize * maxPIN) + itemMargin
        var newht = itemSize + ( itemSize / 2) + itemMargin
        val textBounds = Rect()
        textPaint.getTextBounds("1", 0, 1, textBounds)
        if (textBounds.height() > newht) {
            newht = textBounds.height() + (itemMargin * 2)
        }

        val wM = MeasureSpec.getMode(widthMeasureSpec)
        val wS = MeasureSpec.getSize(widthMeasureSpec)
        val hM = MeasureSpec.getMode(heightMeasureSpec)
        val hS = MeasureSpec.getSize(heightMeasureSpec)

        // Measure Width custom view

        // Measure Width custom view
        val width: Int = when (wM) {
            MeasureSpec.EXACTLY -> {
                // Must be of width size
                wS
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than new
                // width and width size
                newWid.coerceAtMost(wS)
            }
            else -> {
                // Be whatever you want
                newWid
            }
        }

        // Measure Height of custom view

        // Measure Height of custom view
        val height: Int = when (hM) {
            MeasureSpec.EXACTLY -> {
                // Must be of height size
                hS
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than new
                // height and height size
                newht.coerceAtMost(hS)
            }
            else -> {
                // Be whatever you want
                newht
            }
        }

        // for making the desired size

        // for making the desired size
        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paintDebug.color = Color.RED
        paintDebug.style = Paint.Style.STROKE
        paintDebug.strokeWidth = 3f

        //canvas.drawRect(0f, height.toFloat(), width.toFloat(), 0f, paintDebug)

        var ix = itemMargin
        for (i in 0 until  maxPIN) {
            val iy = (height - itemSize) / 2
            val rect = Rect(ix, iy, ix + itemSize , iy + itemSize)
            //canvas.drawRect(rect, paintDebug)
            ix += itemSize + itemMargin

            if (i < mText.length) {
                val textBounds = Rect()
                val t = mText[i].toString()

                if (pinVisibleDuration > 0) {
                    if (chars[i].isShow) {
                        textPaint.getTextBounds(t, 0, t.length, textBounds)
                        canvas.drawText(t, rect.exactCenterX() - ( textBounds.width() / 2), rect.exactCenterY() + ( textBounds.height() / 2), textPaint)
                    } else {
                        itemFocusDrawable?.bounds = rect
                        itemFocusDrawable?.draw(canvas)
                    }
                } else {
                    if (chars[i].isShow) {
                        itemFocusDrawable?.bounds = rect
                        itemFocusDrawable?.draw(canvas)
                        textPaint.getTextBounds(t, 0, t.length, textBounds)
                        canvas.drawText(t, rect.exactCenterX() - ( textBounds.width() / 2), rect.exactCenterY() + ( textBounds.height() / 2), textPaint)
                    }
                }
            } else {
                itemDrawable?.bounds = rect
                itemDrawable?.draw(canvas)
            }
        }
    }

    fun addPinChar(char: Char) {
        if (mText.length >= maxPIN) return
        mText.append(char)
        chars.add(ItemText(char))
        invalidate()
    }

    fun removeChar() {
        if (mText.isNotEmpty()) {
            val item = chars[mText.length - 1].also { it.delete() }
            chars.remove(item)
            mText.delete(mText.length - 1, mText.length)
            invalidate()
        }
    }

    fun clear() {
        chars.forEach { it.delete() }
        chars.clear()
        mText.clear()
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_UP && isKeyboardDefault) {
            val imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
            requestFocus()
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.e("MyInputConnection", "onKeyUp() $keyCode")
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        outAttrs?.inputType = InputType.TYPE_CLASS_NUMBER
        outAttrs?.imeOptions = EditorInfo.IME_ACTION_DONE
        return PinInputConnection(this,true)
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    inner class PinInputConnection(view: PinView, fullEditor: Boolean) : BaseInputConnection(view, fullEditor) {

        private var mEditable = view.mText

        init {
            Selection.setSelection(mEditable, mEditable.length)
        }

        override fun getEditable(): Editable {
            return mEditable
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            Log.e("MyInputConnection", "deleteSurroundingText()")
            if (mText.isNotEmpty()) {
                val item = chars[mText.length - 1].also { it.delete() }
                chars.remove(item)
                invalidate()
            }
            invalidate()
            return super.deleteSurroundingText(beforeLength, afterLength)
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (Character.isDigit(text!![0])) addPinChar(text[0])
            return Character.isDigit(text[0])
        }
    }

    inner class ItemText(val char: Char) {
        var isShow = false
        var mHandler = Handler()

        var mRunnable: Runnable = Runnable {
            isShow = false
            invalidate()
        }

        init {
            isShow = isPinVisible
            if (pinVisibleDuration > 0) {
                mHandler.postDelayed(mRunnable, pinVisibleDuration.toLong())
            }
        }

        fun delete() {
            mHandler.removeCallbacks(mRunnable)
        }
    }
}