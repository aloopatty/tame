package com.ludoglassarena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class LudoView extends View {
    private final Paint p = new Paint(3);
    private final Random random = new Random();
    private final int[] colors = {Color.rgb(232,75,105), Color.rgb(39,190,137), Color.rgb(241,177,61), Color.rgb(77,143,235)};
    private final String[] names = {"RED", "GREEN", "YELLOW", "BLUE"};
    private final int[][] path = {{6,1},{6,2},{6,3},{6,4},{6,5},{5,6},{4,6},{3,6},{2,6},{1,6},{0,6},{0,7},{0,8},{1,8},{2,8},{3,8},{4,8},{5,8},{6,9},{6,10},{6,11},{6,12},{6,13},{6,14},{7,14},{8,14},{8,13},{8,12},{8,11},{8,10},{8,9},{9,8},{10,8},{11,8},{12,8},{13,8},{14,8},{14,7},{14,6},{13,6},{12,6},{11,6},{10,6},{9,6},{8,5},{8,4},{8,3},{8,2},{8,1},{8,0},{7,0},{6,0}};
    private final int[][][] bases = {{{1,1},{1,4},{4,1},{4,4}},{{1,10},{1,13},{4,10},{4,13}},{{10,10},{10,13},{13,10},{13,13}},{{10,1},{10,4},{13,1},{13,4}}};
    private final int[][][] lanes = {{{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}},{{1,7},{2,7},{3,7},{4,7},{5,7},{6,7}},{{7,13},{7,12},{7,11},{7,10},{7,9},{7,8}},{{13,7},{12,7},{11,7},{10,7},{9,7},{8,7}}};
    private final int[] starts = {0,13,26,39};
    private final int[] safe = {0,8,13,21,26,34,39,47};
    private int players = 4, turn = 0, roll = 0, sixes = 0, selectedDice = -1;
    private int[][] tokens = new int[4][4]; // -1 home, 0..51 track, 52..57 home lane, 58 finished
    private boolean winner = false;
    private String message = "Tap the active player's die";
    private float boardX, boardY, cell, boardSize, diceTop, diceH, selectorTop;
    private final RectF[] diceRects = new RectF[4];
    private final RectF[] countRects = new RectF[3];

    public LudoView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); reset(); }
    public void reset() { for (int i=0;i<4;i++) for(int j=0;j<4;j++) tokens[i][j]=-1; turn=0; roll=0; sixes=0; winner=false; message="Tap the active player's die"; invalidate(); }
    private void fill(Canvas c,int color,float l,float t,float r,float b,float rad) { p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(l,t,r,b,rad,rad,p); }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold) { p.setColor(color);p.setTextSize(size);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p); }
    private void stroke(Canvas c,int color,float width,float l,float t,float r,float b,float rad) { p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawRoundRect(l,t,r,b,rad,rad,p);p.setStyle(Paint.Style.FILL); }
    private int alpha(int color,int a){return Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));}

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); float w=getWidth(),h=getHeight();
        canvas.drawColor(Color.rgb(9,16,29));
        // The only controls above the board are the player-count selector.
        selectorTop=dp(13); float selectorH=dp(37); text(canvas,"PLAYERS",dp(19)+dp(27),selectorTop+selectorH*.68f,dp(11),Color.rgb(137,158,183),true);
        float sw=dp(48),sx=dp(116); for(int i=0;i<3;i++){countRects[i]=new RectF(sx+i*(sw+dp(7)),selectorTop,sx+i*(sw+dp(7))+sw,selectorTop+selectorH);fill(canvas,players==i+2?Color.rgb(49,78,110):Color.rgb(21,34,52),countRects[i].left,countRects[i].top,countRects[i].right,countRects[i].bottom,dp(12));stroke(canvas,players==i+2?Color.rgb(113,169,224):Color.rgb(48,65,85),dp(1),countRects[i].left,countRects[i].top,countRects[i].right,countRects[i].bottom,dp(12));text(canvas,String.valueOf(i+2),countRects[i].centerX(),countRects[i].centerY()+dp(5),dp(15),Color.WHITE,true);}
        float availableTop=selectorTop+selectorH+dp(12), bottom=h-dp(10);
        float diceArea=Math.min(dp(150),Math.max(dp(118),h*.19f));
        boardSize=Math.min(w-dp(22),bottom-availableTop-diceArea-dp(14)); boardSize=Math.max(dp(180),boardSize); cell=boardSize/15f; boardX=(w-boardSize)/2f; boardY=availableTop+Math.max(0,(bottom-availableTop-diceArea-dp(14)-boardSize)/2f);
        drawBoard(canvas);
        diceTop=boardY+boardSize+dp(12); diceH=diceArea; drawDice(canvas,w);
    }
    private void drawBoard(Canvas c) {
        float x=boardX,y=boardY,s=boardSize;
        fill(c,Color.rgb(23,38,58),x-dp(3),y-dp(3),x+s+dp(3),y+s+dp(3),dp(17));stroke(c,Color.rgb(65,86,111),dp(1),x-dp(3),y-dp(3),x+s+dp(3),y+s+dp(3),dp(17));
        int[] zone={0,1,2,3};int[][] z={{0,0},{0,9},{9,9},{9,0}};
        for(int i=0;i<4;i++){float l=x+z[i][1]*cell,t=y+z[i][0]*cell;fill(c,alpha(colors[i],65),l+cell*.12f,t+cell*.12f,l+cell*5.88f,t+cell*5.88f,cell*.32f);fill(c,Color.rgb(231,237,246),l+cell*.75f,t+cell*.75f,l+cell*5.25f,t+cell*5.25f,cell*.45f);for(int j=0;j<4;j++){float cx=l+cell*(1.8f+(j%2)*2.4f),cy=t+cell*(1.8f+(j/2)*2.4f);p.setColor(alpha(colors[i],55));c.drawCircle(cx,cy,cell*.68f,p);p.setColor(colors[i]);c.drawCircle(cx,cy,cell*.48f,p);p.setColor(Color.WHITE);c.drawCircle(cx-cell*.12f,cy-cell*.14f,cell*.13f,p);}}
        // Track cells and the eight protected stars.
        for(int i=0;i<52;i++){float[] q=xy(path[i][0],path[i][1]);int col=Color.rgb(237,242,248);if(i==0)col=colors[0];if(i==13)col=colors[1];if(i==26)col=colors[2];if(i==39)col=colors[3];fill(c,col,q[0]-cell*.48f,q[1]-cell*.48f,q[0]+cell*.48f,q[1]+cell*.48f,cell*.08f);stroke(c,Color.rgb(178,192,209),dp(.6f),q[0]-cell*.48f,q[1]-cell*.48f,q[0]+cell*.48f,q[1]+cell*.48f,cell*.08f);if(isSafe(i))text(c,"★",q[0],q[1]+cell*.25f,cell*.65f,Color.rgb(190,144,50),true);}
        for(int i=0;i<4;i++)for(int j=0;j<6;j++){float[] q=xy(lanes[i][j][0],lanes[i][j][1]);fill(c,alpha(colors[i],j==5?240:150),q[0]-cell*.48f,q[1]-cell*.48f,q[0]+cell*.48f,q[1]+cell*.48f,cell*.08f);}
        // Four colored home triangles meet at the center.
        Path tri=new Path();float cx=x+7.5f*cell,cy=y+7.5f*cell;
        tri.moveTo(x+6*cell,y+6*cell);tri.lineTo(x+9*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[1]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+6*cell);tri.lineTo(x+9*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[2]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+9*cell);tri.lineTo(x+6*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[3]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+6*cell,y+9*cell);tri.lineTo(x+6*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[0]);c.drawPath(tri,p);
        drawTokens(c);
    }
    private float[] xy(int row,int col){return new float[]{boardX+(col+.5f)*cell,boardY+(row+.5f)*cell};}
    private boolean isSafe(int v){for(int a:safe)if(a==v)return true;return false;}
    private int global(int player,int progress){return (starts[player]+progress)%52;}
    private float[] tokenPosition(int player,int piece){int v=tokens[player][piece];if(v<0){int[][] b=bases[player];return xy(b[piece][0],b[piece][1]);}if(v>=52&&v<58)return xy(lanes[player][v-52][0],lanes[player][v-52][1]);if(v==58)return xy(7,7);int g=global(player,v);return xy(path[g][0],path[g][1]);}
    private void drawTokens(Canvas c){for(int i=0;i<players;i++)for(int j=0;j<4;j++){float[] q=tokenPosition(i,j);float r=cell*.34f;p.setColor(Color.argb(90,0,0,0));c.drawCircle(q[0]+dp(1),q[1]+dp(2),r*1.12f,p);p.setColor(colors[i]);c.drawCircle(q[0],q[1],r,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));c.drawCircle(q[0],q[1],r,p);p.setStyle(Paint.Style.FILL);p.setColor(alpha(Color.WHITE,150));c.drawCircle(q[0]-r*.27f,q[1]-r*.3f,r*.22f,p);if(i==turn&&roll>0&&legal(i).contains(j)){p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));c.drawCircle(q[0],q[1],r*1.38f,p);p.setStyle(Paint.Style.FILL);}}}
    private void drawDice(Canvas c,float w){float gap=dp(9),pad=dp(12),boxW=(w-pad*2-gap)/2f,boxH=(diceH-gap)/2f;for(int i=0;i<4;i++){int row=i/2,col=i%2;RectF r=new RectF(pad+col*(boxW+gap),diceTop+row*(boxH+gap),pad+col*(boxW+gap)+boxW,diceTop+row*(boxH+gap)+boxH);diceRects[i]=r;if(i>=players){fill(c,Color.rgb(16,26,42),r.left,r.top,r.right,r.bottom,dp(12));continue;}boolean active=i==turn;fill(c,active?Color.rgb(36,58,83):Color.rgb(19,31,48),r.left,r.top,r.right,r.bottom,dp(13));stroke(c,active?colors[i]:Color.rgb(48,66,88),active?dp(2):dp(1),r.left,r.top,r.right,r.bottom,dp(13));p.setColor(colors[i]);c.drawCircle(r.left+dp(17),r.centerY(),dp(5),p);text(c,names[i],r.left+dp(36),r.centerY()+dp(4),dp(10),Color.rgb(172,190,211),true);float dx=r.right-dp(26),dy=r.centerY();fill(c,active?colors[i]:Color.rgb(57,75,98),dx-dp(15),dy-dp(15),dx+dp(15),dy+dp(15),dp(7));text(c,active&&roll>0?String.valueOf(roll):"⚄",dx,dy+dp(6),dp(19),Color.WHITE,true);if(active)text(c,winner?"WINNER":(roll>0?"PICK A TOKEN":"YOUR TURN"),r.centerX(),r.bottom-dp(5),dp(8),Color.rgb(183,203,225),true);}}
    private java.util.ArrayList<Integer> legal(int player){java.util.ArrayList<Integer> a=new java.util.ArrayList<>();for(int j=0;j<4;j++){int v=tokens[player][j];if(v==58)continue;if(v<0?roll==6:v+roll<=58)a.add(j);}return a;}
    private void rollDice(){if(winner||roll!=0)return;int n=1+random.nextInt(6);if(n==6)sixes++;else sixes=0;roll=n;if(sixes>=3){message="Three sixes — turn forfeited";roll=0;sixes=0;next();return;}if(legal(turn).isEmpty()){message=n==6?"No token can move — roll again":"No legal move — turn passed";if(n!=6){roll=0;next();}else roll=0;}}
    private void moveToken(int piece){if(roll==0||!legal(turn).contains(piece))return;int old=tokens[turn][piece];int dest=old<0?0:old+roll;tokens[turn][piece]=dest;boolean captured=false;if(dest<52){int square=global(turn,dest);if(!isSafe(square)){for(int k=0;k<players;k++)if(k!=turn)for(int j=0;j<4;j++)if(tokens[k][j]>=0&&tokens[k][j]<52&&global(k,tokens[k][j])==square){tokens[k][j]=-1;captured=true;}}}boolean won=true;for(int v:tokens[turn])if(v!=58)won=false;if(won){winner=true;message=names[turn]+" WINS";}else if(roll==6||captured){message=captured?"Captured! Roll again":"Six! Roll again";}else{message="";next();}roll=0;}
    private void next(){turn=(turn+1)%players;sixes=0;}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();for(int i=0;i<3;i++)if(countRects[i]!=null&&countRects[i].contains(x,y)){players=i+2;reset();return true;}if(winner){reset();return true;}if(y>=diceTop){for(int i=0;i<players;i++)if(diceRects[i]!=null&&diceRects[i].contains(x,y)){if(i==turn){if(roll==0)rollDice();else message="Choose a glowing token";}invalidate();return true;}}if(roll>0){for(int j=0;j<4;j++){float[] q=tokenPosition(turn,j);if(legal(turn).contains(j)&&Math.hypot(x-q[0],y-q[1])<cell*.75f){moveToken(j);invalidate();return true;}}}invalidate();return true;}
}
