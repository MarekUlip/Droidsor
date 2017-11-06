package com.example.marekulip.droidsor.grapview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Fredred on 03.09.2017.
 */

public class LineGraphView extends View implements GestureDetector.OnGestureListener{
    private static final String TAG = LineGraphView.class.toString();

    private GestureDetectorCompat mDetector;

    /**
     * Whole disposable width of a view
     */
    private int width;
    private int height;
    /**
     * Width on which the graph items are drawn (meaning graph without surroundings like labels, names, etc...)
     */
    private int drawableWidth;
    private int drawableHeight;

    /**
     * Width of rectangle representing single line color.
     */
    private float colorRectWidth;
    private float colorRectOffset;

    private float offsetLeft;
    private float offsetBottom;
    private float offsetTop = 10;
    private float offsetRight = 10;
    /**
     * Bottom position of drawable area for lines in graph. It is not neccessary to have it but I had to count
     * it on multiple locations so I decided to make it class variable. It is used to help draw anything that's under graph
     */
    private float graphBottom;

    /**
     * Start and end positions for graph vertical description val line. Under this line is actual val description
     */
    private float xValLabYStartPos;
    private float xValLabYEndPos;

    /**
     * Starting y position for axis x name
     */
    private float axisNameYPos;


    private float valTextHeight;
    private float labelTextHeight;
    private float graphNameTextHeight;

    private float strokeWidth = 2;

    private final Rect bounds = new Rect();

    private int xItemsDisplayCount = 4;
    private int yItemsDisplayCount = 4;
    private int breakIndexX;
    private float breakNumY;

    private float itemDrawStepX;
    private float itemDrawStepY;

    private final Paint bgColor = new Paint();
    private final Paint lineColor = new Paint();
    //private static final Paint graphLineColor = new Paint();//TODO colors for multiple lines
    private List<Paint> graphLineColors = new ArrayList<>();
    private final Paint valsTextColor = new Paint(); //TODO resolve text color and size, also think about static
    private final Paint axisLabelsTextColor = new Paint();
    private final Paint graphNameTextColor = new Paint();

    /**
     * List with most items. This list will lead the line meaning this line will be the last one to see. Other lines can disapear but this one stays.
     */
    private List<Entry> leadGraphItems;
    private List<List<Entry>> dataSets = new ArrayList<>();//TODO draw for multiple lines
    private int indexToCheck = -1;
    //private List<Entry> sortedGraphItems;
    private int scope = 500;
    /**
     * Used for proper value positioning
     */
    private float lowestItem = Integer.MAX_VALUE;
    private float highestItem = Integer.MIN_VALUE;
    private int itemValueSpan = 0;
    /**
     * First visible position in the list (used when scrolling)
     */
    private int actualPos;

    private String xAxisLabel = null;
    private String yAxisLabel = null;
    private String graphName = null;
    private List<String> lineNames = null;

    private static int classCount = 0;


    public LineGraphView(Context context) {
        this(context,(List<Entry>)null);
        Log.d(TAG, "LineGraphView: sd");
        /*super(context);
        attachObserver();*/
    }

    public LineGraphView(Context context, List<Entry> items) {
        super(context);
        init(context,items);
    }

    public LineGraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context,null);
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,null);
    }

    private void init(Context context,List<Entry> items){
        Log.d(TAG, "init: "+ ++classCount);
        setUpColors();
        attachObserver();
        leadGraphItems = items;
        if(leadGraphItems !=null){
            countItemsMeasures();
        }
        mDetector = new GestureDetectorCompat(context,this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBaseLines(canvas);
        drawTexts(canvas);
        drawItems(canvas);
        drawLineNames(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 50;
        int desiredHeight = 300;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return true;//super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true; //TODO possible error
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        /*Log.d(TAG, "onScroll: "+v+" "+v1);
        Log.d(TAG, "onScroll: " + motionEvent.toString() + motionEvent1.toString());*/
        changeActualPosition((int)(15+Math.abs(v/100))*(int)(Math.signum(motionEvent.getX()-motionEvent1.getX())));
        return true;//TODO Careful
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.d(TAG, "onFling: "+v+" "+v1);
        Log.d(TAG, "onFling: " + motionEvent.toString() + motionEvent1.toString());
        int sign = (int)Math.signum(motionEvent.getX()-motionEvent1.getX());
        float absVal = Math.abs(v);
        Log.d(TAG, "onFling: "+(absVal/1000)*10);
        if(absVal>1000){
            for(int i = 0; i < (absVal/1000);i++){
                changeActualPosition(15*sign);
            }
        }
        return true;
    }

    /**
     * Counts measures for placing individual graph items
     */
    private void countItemsMeasures(){
        countItemsStep();
        countBreakIndex();
    }

    /**
     * Attach observer to itself so it can get width after it is known
     */
    private void attachObserver(){
        Log.d(TAG, "attachObserver: ");
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
        {
            @Override
            public boolean onPreDraw()
            {
                getViewTreeObserver().removeOnPreDrawListener(this);
                width = LineGraphView.this.getWidth();
                height = LineGraphView.this.getHeight();
                Log.d(TAG, "onPreDraw: Observer running");
                countMeasures();
                return false;
            }
        });
    }

    private void setUpColors(){
        bgColor.setColor(Color.WHITE);
        lineColor.setColor(Color.GRAY);
        lineColor.setStrokeWidth(3);
        /*graphLineColor.setColor(Color.RED);
        graphLineColor.setStrokeWidth(2);*/

        valsTextColor.setColor(Color.BLACK);
        valsTextColor.setTextSize(30f);

        valsTextColor.getTextBounds("W",0,1,bounds);
        valTextHeight = bounds.height();

        axisLabelsTextColor.setColor(Color.BLACK);
        axisLabelsTextColor.setTextSize(30f);

        axisLabelsTextColor.getTextBounds("LABEL",0,5,bounds);
        labelTextHeight = bounds.height();

        graphNameTextColor.setColor(Color.BLACK);
        graphNameTextColor.setTextSize(40f);

        graphNameTextColor.getTextBounds("GRAPH",0,5,bounds);
        graphNameTextHeight = bounds.height();

        Log.d(TAG, "setUpColors: Val: "+valTextHeight+" lab: "+ labelTextHeight);
    }

    /**
     * Counts basic measures of offsets so lines can be drawn correctly
     */
    private void countMeasures(){
        if(width<400){
            offsetLeft = 20;
            offsetRight = 10;
            scope = 50;
        }else {
            offsetLeft = width*(float)0.05;
            offsetRight = width*(float)0.02;
            scope = width /4;
        }
        if(height<200){
            offsetBottom = 20;
            offsetTop = 10;
        }else {
            offsetBottom = height*(float)0.05;
            offsetTop = height*(float)0.02;
        }

        if(lineNames!=null){
            colorRectWidth = 2*valTextHeight;
            colorRectOffset = width*(float)0.01;
            if(colorRectOffset <1) colorRectOffset = 1;
        }else {
            colorRectWidth = 0;
            Log.d(TAG, "countMeasures: ");
        }


        offsetBottom+= labelTextHeight + colorRectWidth;
        offsetLeft += labelTextHeight;
        offsetTop += labelTextHeight;
        drawableWidth = (int)(width - (offsetLeft+offsetRight));
        //Log.d(TAG, "countMeasures: Drawble wdth"+ drawableWidth);
        drawableHeight = (int)(height - (offsetTop+offsetBottom));
        graphBottom = height-offsetBottom;

        xValLabYStartPos = graphBottom-(offsetBottom/4);
        xValLabYEndPos = graphBottom+(offsetBottom/4);

        axisNameYPos = xValLabYEndPos + (valTextHeight+labelTextHeight);
        countItemsStep();
    }

    /**
     * Counts pixel step of each item from the last one
     */
    private void countItemsStep(){
        if(leadGraphItems != null){
            if(leadGraphItems.size()>scope){
                itemDrawStepX = (float)drawableWidth/scope;
            }
            else itemDrawStepX = (float)drawableWidth/ leadGraphItems.size();

            /*int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;*/
            float val;

            for (Entry e : dataSets.get(indexToCheck)) {
                val = e.getyValue();
                if (val > highestItem) {
                    highestItem = val;
                }
                if (val < lowestItem) {
                    lowestItem = val;
                }
            }

            //lowestItem = lowest;
            itemValueSpan = (int)(highestItem-lowestItem);
            itemDrawStepY = (float)drawableHeight/(itemValueSpan); //TODO draws small graphs with higher numbers (perhaps integer dividing)
            breakNumY = itemValueSpan/yItemsDisplayCount;
        }
    }

    /**
     * Break index is position in the list of graph items at which the description will be written
     */
    private void countBreakIndex(){
        if(leadGraphItems != null){
            if(leadGraphItems.size()<scope) breakIndexX = leadGraphItems.size() /xItemsDisplayCount; //scope / xItemsDisplayCount;
            else breakIndexX = scope / xItemsDisplayCount;
            if(breakIndexX==0)breakIndexX = 1;
        }
    }

    private void changeActualPosition(int change){
        if(leadGraphItems !=null){
            if(leadGraphItems.size()<scope)return;
            int pos = actualPos+change;
            if(pos<0){
                actualPos = 0;
            }
            else if(pos> leadGraphItems.size()-scope/2){
                actualPos= leadGraphItems.size()-scope/2;
            }
            else {
                actualPos = pos;
            }
            invalidate();
        }
    }

    //TODO scope very dependent on screen another things are width of lines offsets, ends of lines, labels, check it out if needed
    private void drawItems(Canvas canvas){
        if(leadGraphItems != null){
            int colorPos = 0;
            for(List<Entry> graphItems : dataSets){
                int end = actualPos + scope;
                if(end > graphItems.size())end = graphItems.size();
                int j = 0;
                for(int i = actualPos; i<end-1;i++){
                    canvas.drawLine(
                            offsetLeft+(itemDrawStepX*j),
                            graphBottom - ((graphItems.get(i).getyValue()-lowestItem)*itemDrawStepY),
                            offsetLeft+(itemDrawStepX*(j+1)),
                            graphBottom - ((graphItems.get(i+1).getyValue()-lowestItem)*itemDrawStepY),
                            graphLineColors.get(colorPos));


                    if(i%breakIndexX==0){
                        canvas.drawLine(
                                getXValLabX(j),
                                xValLabYStartPos,
                                getXValLabX(j),
                                xValLabYEndPos,
                                lineColor);
                        canvas.drawText(graphItems.get(i).getXValue(),getXValLabX(j)+5, xValLabYEndPos, valsTextColor);
                    }
                    j++;
                }
                if(end== leadGraphItems.size()){
                    canvas.drawLine(
                            getXValLabX(j),
                            xValLabYStartPos,
                            getXValLabX(j),
                            xValLabYEndPos,
                            lineColor
                    );
                    canvas.drawText(leadGraphItems.get(end-1).getXValue(),getXValLabX(j)+5, xValLabYEndPos, valsTextColor);
                }
                colorPos++;
            }
            float yValLabXStart = offsetLeft-(offsetLeft-offsetLeft/2);
            float yValLabXEnd = offsetLeft+(offsetLeft-offsetLeft/2);
            float yValLabY;
            for(int i = 0; i<yItemsDisplayCount+1; i++){
                yValLabY = graphBottom - ((breakNumY*i)*itemDrawStepY);
                canvas.drawLine(
                        yValLabXStart,
                        yValLabY,
                        yValLabXEnd,
                        yValLabY,
                        lineColor
                );//TODO make value be drawn precisely
                //TODO Y axis is not fully used
                canvas.drawText(String.valueOf((int)(lowestItem+(breakNumY*i))),yValLabXStart,yValLabY, valsTextColor);
            }
        }
    }

    private float getXValLabX(int j){
        return offsetLeft+(itemDrawStepX*j);
    }

    private void drawBaseLines(Canvas canvas){
        canvas.drawRect(0,0,width,height,bgColor);
        canvas.drawLine(offsetLeft,offsetTop,offsetLeft,height-offsetBottom+10,lineColor);
        canvas.drawLine(offsetLeft-10,height-offsetBottom,width-offsetRight,height-offsetBottom,lineColor);
    }

    private void drawTexts(Canvas canvas){
        if(xAxisLabel!=null){
            axisLabelsTextColor.getTextBounds(xAxisLabel,0,xAxisLabel.length(),bounds);
            canvas.drawText(xAxisLabel,(width/2)-(bounds.width()/2), axisNameYPos,axisLabelsTextColor);//TODO compute right position
        }
        if(yAxisLabel!=null){
            axisLabelsTextColor.getTextBounds(yAxisLabel,0,yAxisLabel.length(),bounds);
            canvas.save();
            canvas.rotate(270f,labelTextHeight+5,height/2+bounds.width()/2);
            canvas.drawText(yAxisLabel,labelTextHeight+5,height/2+bounds.width()/2,axisLabelsTextColor);
            canvas.restore();
        }
        if(graphName!=null){
            graphNameTextColor.getTextBounds(graphName,0,graphName.length(),bounds);
            canvas.drawText(graphName,width/2-bounds.width()/2,graphNameTextHeight,graphNameTextColor);
        }
    }

    private void drawLineNames(Canvas canvas){
        if(lineNames == null)return;
        float startY = axisNameYPos + labelTextHeight*2+10;
        float startX = 0;
        String text;
        for(int i = 0;i<lineNames.size();i++){
            text = lineNames.get(i);
            valsTextColor.getTextBounds(text,0,text.length(),bounds);
            startX+=bounds.width()+colorRectWidth+ colorRectOffset;
        }
        startX = (width/2)-(startX/2);
        for(int i = 0;i<graphLineColors.size();i++){
            text = lineNames.get(i);
            canvas.drawRect(startX,startY-valTextHeight*2,startX+colorRectWidth,startY,graphLineColors.get(i));
            canvas.drawText(text,startX+colorRectWidth+ colorRectOffset,startY-valTextHeight/2,axisLabelsTextColor);
            valsTextColor.getTextBounds(text,0,text.length(),bounds);
            startX += colorRectWidth+ colorRectOffset *2+bounds.width();
        }
    }

    public void setXItemsDisplayCount(int displayCount){
        if(displayCount > 8) xItemsDisplayCount = 8;
        else xItemsDisplayCount = displayCount;
    }

    public void setYItemsDisplayCount(int displayCount){
        if(displayCount > 10) yItemsDisplayCount = 10;
        else yItemsDisplayCount = displayCount;
    }

    /*public void setLeadGraphItems(List<Entry> items){
        leadGraphItems = items;
        countItemsMeasures();
        invalidate();
    }*/

    /**
     * Adds new items representing one line in a graph. If colors werent set before or count of colors is smaller
     * than count of lines red color is added.
     * @param items
     */
    public void addGraphItems(List<Entry> items){
        indexToCheck++;
        dataSets.add(items);
        if(graphLineColors.size() < dataSets.size()){
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setStrokeWidth(strokeWidth);
            graphLineColors.add(p);
        }
        if(leadGraphItems == null){
            leadGraphItems = items;
        }
        if(items.size()>leadGraphItems.size())leadGraphItems=items;
        Log.d(TAG, "addGraphItems: "+dataSets.size());
        Log.d(TAG, "addGraphItems: "+ indexToCheck);
        countItemsMeasures();
        invalidate();
    }


    /**
     * For use in adapters (If only add items was used items were added multiple times)
     * @param items
     */
    public void setGraphItems(List<List<Entry>> items){
        clearItems();
        for(List<Entry> e:items){
            addGraphItems(e);
        }
    }

    /**
     * Test puropses only
     * @param items
     */
    public void setGraphItem(List<Entry> items){
        clearItems();
        addGraphItems(items);
    }

    private void clearItems(){
        indexToCheck = -1;
        leadGraphItems = null;
        dataSets.clear();
    }

    //public void changeGraphLineColor(int color){
       // graphLineColor.setColor(color);
    //}

    public void changeGraphLineColor(int color, int index){
        if(graphLineColors.size()<=index)return;
        Paint p = new Paint();
        p.setColor(color);
        p.setStrokeWidth(strokeWidth);
        graphLineColors.set(index,p);
        invalidate();
    }

    /**
     * Sets coloros to be used with the lines. If the number of provided colors is smaller than the number of lines
     * colors will be repeated from provided list of colors till colors count matches line's count
     * @param colors list of colors
     */
    public void setGraphLineColors(List<Paint> colors){
        graphLineColors = colors;
        int colorPos = 0;
        while(graphLineColors.size()<dataSets.size()){
            Paint p = new Paint();
            p.setColor(graphLineColors.get(colorPos).getColor());
            p.setStrokeWidth(strokeWidth);
            graphLineColors.add(p);
            colorPos++;
        }
        invalidate();
    }

    public void setGraphLineNames(List<String> names){
        lineNames = names;
        int namesCount = lineNames.size();
        while(namesCount<dataSets.size()){
            lineNames.add("Line #"+namesCount+1);
            namesCount++;
        }
        invalidate();
    }

    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }

    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }
}
