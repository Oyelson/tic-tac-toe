package com.oyegbite.tictactoe.ticTacToe

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.oyegbite.tictactoe.R
import com.oyegbite.tictactoe.utils.AppUtils
import com.oyegbite.tictactoe.utils.Constants
import com.oyegbite.tictactoe.utils.SharedPreference
import kotlin.math.floor


class TicTacToeBoard(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    companion object {
        private val TAG: String = TicTacToeBoard::class.java.simpleName
    }

    private var mTicTacToeDataListener: TicTacToeDataListener? = null

    fun initListener(ticTacToeDataListener: TicTacToeDataListener) {
        mTicTacToeDataListener = ticTacToeDataListener
    }

    private var mCountDownTimer: CountDownTimer? = null
    private var mSharedPreference: SharedPreference = SharedPreference(context)
    private val mXColor: Int
    private val mOColor: Int
    private val mWinningLineColorPlayerOne: Int
    private val mWinningLineColorPlayerTwo: Int
    private var mBoardDimension: Int = 3 // 3 x 3 board
    private var mBoardWidth: Int = width
    private val mBoardColor: Int
    private var mBoardThickness: Float = 8F
    private var mMarkerThickness: Float = 24F
    private var mCellSize: Int = width / mBoardDimension
    private var mCellPadding: Float = 0.1F * mCellSize
    private val mPaint: Paint = Paint()
    private var mAttributeArray: TypedArray = context.theme.obtainStyledAttributes(
        attrs,
        R.styleable.TicTacToeBoard,
        0,
        0
    )

    var mGameLogic: TicTacToeLogic = TicTacToeLogic(context, mBoardDimension)

    init {

        try {
            mBoardColor =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_boardColor, 0)

            mWinningLineColorPlayerOne =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_winningLineColorPlayerOne, 0)

            mWinningLineColorPlayerTwo =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_winningLineColorPlayerTwo, 0)

            mXColor =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_xColor, 0)

            mOColor =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_oColor, 0)

            mBoardDimension =
                mAttributeArray.getInteger(R.styleable.TicTacToeBoard_boardRowByCol, 3)

        } finally {
            mAttributeArray.recycle()
        }
    }

    fun setMovesPlayed(moves: ArrayList<IntArray>) {
        mGameLogic.setMovesPlayed(moves)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val spaceAroundBoard = 32
        // Get minimum dimension from the width and height of player's device,
        // and also remove required space.
        mBoardWidth = measuredHeight.coerceAtMost(measuredWidth) - spaceAroundBoard

        mCellSize = mBoardWidth / mBoardDimension
        mCellPadding = 0.15F * mCellSize

        setMeasuredDimension(mBoardWidth, mBoardWidth) // We need a Square TicTacToa board
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        mPaint.style = Paint.Style.STROKE
        mPaint.isAntiAlias = true

        drawGameBoard(canvas)
        drawMarkers(canvas)
        drawWinningLine(canvas)
    }

    private fun isAITurnToPlay(): Boolean {
        val withAI = !AppUtils.isTwoPlayerMode(TAG, mSharedPreference)
        val isPlayerOneTurn = mSharedPreference.getValue(
            Boolean::class.java,
            Constants.KEY_IS_PLAYER_ONE_TURN,
            true
        ) == true
        return withAI && !isPlayerOneTurn
    }

    fun aiPlays() {
        if (isAITurnToPlay()) {
            if (mGameLogic.boardHasEmptyCell()) {
                aiWaitThenPlay()
            }
        }
    }

    private fun aiWaitThenPlay() {
        val countDownInterval: Long = 500
        val stopInterval: Long = 1500

        mCountDownTimer = object : CountDownTimer(
            stopInterval,
            countDownInterval
        ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                val aiMove = mGameLogic.getAvailableMove()
                play(aiMove)
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val xPos = event.x
        val yPos = event.y

        // Calculate row and col in relation to the x and y position that the user clicked
        if (event.action == MotionEvent.ACTION_DOWN) {
            val row = floor(yPos / mCellSize).toInt()
            val col = floor( xPos / mCellSize).toInt()
            val move = intArrayOf(row, col)

            // User can play move if we have 2 players or 1 player and AI is not currently playing
            if (!isAITurnToPlay()) {
                play(move)
                aiPlays() // Allow AI to play after first player have played if possible
            }
            return true
        }

        return false
    }

    private fun play(move: IntArray) {
        // Game can be over when someone wins and board is not filled
        val isGameOver = mSharedPreference.getValue(
            Boolean::class.java,
            Constants.KEY_IS_GAME_OVER,
            false
        ) == true

        Log.i(TAG, "move = ${move.contentToString()}")

        if (!isGameOver && mGameLogic.addToMoves(move)) {
            val winState = mGameLogic.getGameWinState(mGameLogic.getMovesPlayed())
            checkIfGameOverAfterPlay(winState)
            switchPlayer()
            invalidate() // Redraw game board so as to update new draws
        }
    }

    private fun vibrateDevice(vibrateMills: Long) {
        mTicTacToeDataListener?.vibrateDevice(vibrateMills)
    }

    /**
     * @param winState which is either "Draw", "Pending", "Player 1", "Player 2"
     *  @see R.string.draw
     *  @see R.string.pending
     *  @see R.string.first_player
     *  @see R.string.second_player
     */
    private fun checkIfGameOverAfterPlay(winState: String) {
        Log.i(TAG, "winState = '$winState'")
        if (winState == context.getString(R.string.pending)) {
            vibrateDevice(100L)
            return
        }
        mTicTacToeDataListener?.updateGameOverState(true, winState)
        if (winState == context.getString(R.string.draw)) {
            vibrateDevice(100L)
            return
        }
        vibrateDevice(500L) // A player won hence longer vibration
    }

    private fun switchPlayer() {
        mTicTacToeDataListener?.updateNextPlayerTurn()
    }

    fun clear() {
        mGameLogic.clearBoard()
        invalidate()

        val isPlayerOneTurn = mSharedPreference.getValue(
            Boolean::class.java,
            Constants.KEY_IS_PLAYER_ONE_TURN,
            true
        ) == true
        if (isPlayerOneTurn) {
            mGameLogic.updateWhoPlaysFirst(context.getString(R.string.first_player))
        } else {
            mGameLogic.updateWhoPlaysFirst(context.getString(R.string.second_player))
            aiPlays() // AI would play if user is playing with AI
        }
    }

    private fun drawGameBoard(canvas: Canvas?) {
        mPaint.color = mBoardColor
        mPaint.strokeWidth = mBoardThickness
        //        a  b
        //     ----------
        //     |  |  |  |
        //     |  |  |  |
        //     |__|__|__|
        //        a  b
        for (col in 1 until mBoardDimension) { // Draws line a-a and b-b
            canvas?.drawLine(
                (mCellSize * col).toFloat(),
                0F,
                (mCellSize * col).toFloat(),
                mBoardWidth.toFloat(),
                mPaint
            )
        }

        //      --------
        //   c |________| c
        //   d |________| d
        //     |________|
        for (row in 1 until mBoardDimension) { // Draws line c-c and d-d
            canvas?.drawLine(
                0F,
                (mCellSize * row).toFloat(),
                mBoardWidth.toFloat(),
                (mCellSize * row).toFloat(),
                mPaint
            )
        }
    }

    private fun drawMarkers(canvas: Canvas?) {
        val playerOneSide = mSharedPreference.getValue(
            String::class.java,
            Constants.KEY_PLAYER_1_SIDE,
            Constants.VALUE_PLAYER_1_SIDE_X
        )

        Log.i(TAG, "playerOneSide = '$playerOneSide'")

        val firstPlayer = mSharedPreference.getValue(
            String::class.java,
            Constants.KEY_FIRST_PLAYER,
            context.getString(R.string.first_player)
        )

        val gameMoves = mGameLogic.getMovesPlayed()
        for (i in 0 until gameMoves.size) {
            val (row, col) = gameMoves[i]

            if (firstPlayer == context.getString(R.string.first_player)) {
                drawFirstPlayerMarker(canvas, row, col, i, playerOneSide!!)
            } else {
                drawSecondPlayerMarker(canvas, row, col, i, playerOneSide!!)
            }
        }
    }

    private fun drawFirstPlayerMarker(
        canvas: Canvas?,
        row: Int,
        col: Int,
        moveIdx: Int,
        playerOneSide: String
    ) {
        if (moveIdx % 2 == 0) { // Moves for 1st Player
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                drawX(canvas, row, col)
            } else {
                drawO(canvas, row, col)
            }

        } else { // Moves for 2nd Player
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                drawO(canvas, row, col)
            } else {
                drawX(canvas, row, col)
            }
        }
    }

    private fun drawSecondPlayerMarker(
        canvas: Canvas?,
        row: Int,
        col: Int,
        moveIdx: Int,
        playerOneSide: String
    ) {
        if (moveIdx % 2 == 0) { // Moves for 1st Player
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                drawO(canvas, row, col)
            } else {
                drawX(canvas, row, col)
            }

        } else { // Moves for 2nd Player
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                drawX(canvas, row, col)
            } else {
                drawO(canvas, row, col)
            }
        }
    }

    /**
     * @param row is the row cell number (zero-indexing) on the board
     * @param col is the column cell number (zero-indexing) on the board
     * i.e board[row][col]
     */
    private fun drawX(canvas: Canvas?, row: Int, col: Int) {
        mPaint.color = mXColor
        mPaint.strokeWidth = mMarkerThickness

        canvas?.drawLine( // draw "\"
            (mCellSize * col).toFloat() + mCellPadding,
            (mCellSize * row).toFloat() + mCellPadding,
            (mCellSize * (col + 1)).toFloat() - mCellPadding,
            (mCellSize * (row + 1)).toFloat() - mCellPadding,
            mPaint
        )
        canvas?.drawLine( // draw "/"
            (mCellSize * (col + 1)).toFloat() - mCellPadding,
            (mCellSize * row).toFloat() + mCellPadding,
            (mCellSize * (col)).toFloat() + mCellPadding,
            (mCellSize * (row + 1)).toFloat() - mCellPadding,
            mPaint
        )
    }

    private fun drawO(canvas: Canvas?, row: Int, col: Int) {
        mPaint.color = mOColor
        mPaint.strokeWidth = mMarkerThickness

        // The X coordinate of the left side of the rectangle
        val left = (col * mCellSize) + mCellPadding
        // The Y coordinate of the top of the rectangle
        val top = (row * mCellSize) + mCellPadding
        // The X coordinate of the right side of the rectangle
        val right = (left + mCellSize) - (2 * mCellPadding)
        // The Y coordinate of the bottom of the rectangle
        val bottom = (top + mCellSize) - (2 * mCellPadding)

        canvas?.drawOval(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            mPaint
        )
    }

    /**
     * Only draws winning line if we have a winner
     */
    private fun drawWinningLine(canvas: Canvas?) {
        val firstPlayer = mSharedPreference.getValue(
            String::class.java,
            Constants.KEY_FIRST_PLAYER,
            context.getString(R.string.first_player)
        )
        mGameLogic.updateWhoPlaysFirst(firstPlayer!!)
        // You have to set who plays first above [mGameLogic.updateWhoPlaysFirst(firstPlayer!!)]
        // before you can call the [mGameLogic.getGameWinState(...)] below.
        val winState = mGameLogic.getGameWinState(mGameLogic.getMovesPlayed())
        if (winState == context.getString(R.string.pending)
            || winState == context.getString(R.string.draw)) {
            return
        }

        // Draws winning line if only a player won.
        val winDirection = mGameLogic.getWinDirection()
        val (row, col) = mGameLogic.getFirstWinMove()

        when (winDirection) {
            context.getString(R.string.win_direction_row) ->
                drawRowWinningLine(canvas, row)
            context.getString(R.string.win_direction_col) ->
                drawColWinningLine(canvas, col)
            context.getString(R.string.win_direction_lead_diagonal) ->
                drawLeadingDiagonalWinningLine(canvas)
            context.getString(R.string.win_direction_opp_lead_diagonal) ->
                drawOppositeLeadingDiagonalWinningLine(canvas)
        }

    }

    private fun drawRowWinningLine(canvas: Canvas?, row: Int) {
        setWinningMarkerStrokeWidthAndColor()
        canvas?.drawLine(
            0F,
            (mCellSize * row).toFloat() + (mCellSize / 2),
            mBoardWidth.toFloat(),
            (mCellSize * row).toFloat() + (mCellSize / 2),
            mPaint
        )
    }

    private fun drawColWinningLine(canvas: Canvas?, col: Int) {
        setWinningMarkerStrokeWidthAndColor()
        canvas?.drawLine(
            (mCellSize * col).toFloat() + (mCellSize / 2),
            0F,
            (mCellSize * col).toFloat() + (mCellSize / 2),
            mBoardWidth.toFloat(),
            mPaint
        )
    }

    private fun drawLeadingDiagonalWinningLine(canvas: Canvas?) {
        // The Leading Diagonal has the same row and col indexes
        setWinningMarkerStrokeWidthAndColor()
        canvas?.drawLine(
            0F,
            0F,
            mBoardWidth.toFloat(),
            mBoardWidth.toFloat(),
            mPaint
        )
    }

    private fun drawOppositeLeadingDiagonalWinningLine(canvas: Canvas?) {
        // The Leading Diagonal has the same row and col indexes
        // PS: Leading Diagonal also tells us that row + col == mBoardDimension - 1
        setWinningMarkerStrokeWidthAndColor()
        canvas?.drawLine(
            mBoardWidth.toFloat(),
            0F,
            0F,
            mBoardWidth.toFloat(),
            mPaint
        )
    }

    private fun setWinningMarkerStrokeWidthAndColor() {
        mPaint.strokeWidth = 0.5F * mMarkerThickness

        val playerOneSide = mSharedPreference.getValue(
            String::class.java,
            Constants.KEY_PLAYER_1_SIDE,
            Constants.VALUE_PLAYER_1_SIDE_X
        )
        val isPlayerOneTurn = mSharedPreference.getValue(
            Boolean::class.java,
            Constants.KEY_IS_PLAYER_ONE_TURN,
            true
        ) == true

        // Choose appropriate color for player
        if (isPlayerOneTurn) { // Means it is Player 2 that won.
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                mPaint.color = mOColor
            } else {
                mPaint.color = mXColor
            }
        } else { // Means it is Player 1 that won.
            if (playerOneSide == Constants.VALUE_PLAYER_1_SIDE_X) {
                mPaint.color = mXColor
            } else {
                mPaint.color = mOColor
            }
        }
    }

    fun cancelAIPlay() {
        mCountDownTimer?.let {
            it.cancel()
            mCountDownTimer = null
        }
    }

    private fun drawOImage() { // Not working yet
//        val originalBitmapO = BitmapFactory.decodeResource(resources,
//            R.drawable.ic_baseline_gps_not_fixed_24
//        )
//
//        // The X coordinate of the left side of the rectangle
//        val left = (col * mCellSize) + mCellPadding
//        // The Y coordinate of the top of the rectangle
//        val top = (row * mCellSize) + mCellPadding
//        // The X coordinate of the right side of the rectangle
//        val right = (left + mCellSize) - (2 * mCellPadding)
//        // The Y coordinate of the bottom of the rectangle
//        val bottom = (top + mCellSize) - (2 * mCellPadding)
//        val rectF = RectF(left, top, right, bottom)
//
//        // Working!!!
//        val d = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_gps_not_fixed_24, null)
//        d?.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
//        d?.draw(canvas!!)
//
//        // Not working!!!
//        val resizedBitmapO = Bitmap.createScaledBitmap(
//            originalBitmapO, (mCellSize - mCellPadding).toInt(), (mCellSize - mCellPadding).toInt(), false
//        )
//
//        canvas?.drawBitmap(originalBitmapO, 0F, 0F, null)
//        canvas?.drawBitmap(resizedBitmapO, left.toFloat(), top.toFloat(), null)

//        canvas?.drawBitmap(
//            resizedBitmapO,
//            null,
//            rectF,
//            null
//        )

//        originalBitmapO.recycle()
    }
}