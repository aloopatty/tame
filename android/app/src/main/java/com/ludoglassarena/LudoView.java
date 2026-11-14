package com.ludoglassarena;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LudoView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random=new Random();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final int[] colors={Color.rgb(255,83,105),Color.rgb(54,216,151),Color.rgb(245,196,77),Color.rgb(91,166,255)};
    private final String[] names={"RED","GREEN","YELLOW","BLUE"};
    private final int[][] path={{6,1},{6,2},{6,3},{6,4},{6,5},{5,6},{4,6},{3,6},{2,6},{1,6},{0,6},{0,7},{0,8},{1,8},{2,8},{3,8},{4,8},{5,8},{6,9},{6,10},{6,11},{6,12},{6,13},{6,14},{7,14},{8,14},{8,13},{8,12},{8,11},{8,10},{8,9},{9,8},{10,8},{11,8},{12,8},{13,8},{14,8},{14,7},{14,6},{13,6},{12,6},{11,6},{10,6},{9,6},{8,5},{8,4},{8,3},{8,2},{8,1},{8,0},{7,0},{6,0}};
    private final int[][][] bases={{{1,1},{1,4},{4,1},{4,4}},{{1,10},{1,13},{4,10},{4,13}},{{10,10},{10,13},{13,10},{13,13}},{{10,1},{10,4},{13,1},{13,4}}};
    private final int[][][] lanes={{{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}},{{1,7},{2,7},{3,7},{4,7},{5,7},{6,7}},{{7,13},{7,12},{7,11},{7,10},{7,9},{7,8}},{{13,7},{12,7},{11,7},{10,7},{9,7},{8,7}}};
    private final int[] starts={0,13,26,39},safe={0,8,13,21,26,34,39,47};
    // progress: -1 base, 0..51 outer track, 52..56 home lane, 57 finished.
    private final int[][] tokens=new int[4][4];
    private final int[] captures=new int[4],sixRolls=new int[4];
    private int players=4,turn=0,dice=0,sixChain=0,seconds=15;
    private boolean paused=false,winner=false,sound=true,animating=false;
    private String message="Roll a six to leave your base";
    private float boardX,boardY,boardSize,cell,headerBottom;
    private final RectF[] huds=new RectF[4],rollRects=new RectF[4],playerSelectors=new RectF[3];
    private RectF pauseRect=new RectF(),soundRect=new RectF(),rulesRect=new RectF();
    private float animatedScale=1f;
    private ToneGenerator tone;
    private final Runnable countdown=new Runnable(){@Override public void run(){if(paused||winner||animating)return;seconds--;if(seconds<=0){if(dice!=0){dice=0;message="Time expired — turn passed";nextTurn();}else{message="Time expired — turn passed";nextTurn();}}invalidate();if(!paused&&!winner)handler.postDelayed(this,1000);}};
    public LudoView(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,35);}catch(Exception ignored){}reset();}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    public void reset(){for(int a=0;a<4;a++){captures[a]=0;sixRolls[a]=0;for(int b=0;b<4;b++)tokens[a][b]=-1;}turn=0;dice=0;sixChain=0;seconds=15;paused=false;winner=false;animating=false;message="Roll a six to leave your base";handler.removeCallbacks(countdown);handler.postDelayed(countdown,1000);invalidate();}
    private void fill(Canvas c,int color,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,rad,rad,p);}
    private void stroke(Canvas c,int color,float width,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawRoundRect(l,t,r,b,rad,rad,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
    private int alpha(int color,int a){return Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(Color.rgb(7,13,24));
        drawHeader(c,w);
        float hudH=dp(77),gap=dp(9),available=Math.max(dp(190),h-dp(35)-hudH*2-gap*2);
        boardSize=Math.min(w-dp(18),available);cell=boardSize/15f;boardX=(w-boardSize)/2f;boardY=(h-boardSize)/2f+dp(4);
        drawBoard(c);
        drawHud(c,w,hudH,gap);
        if(paused||winner)drawOverlay(c,w,h);
    }
    private void drawHeader(Canvas c,float w){text(c,"LUDO  GLASS ARENA",w/2,dp(23),dp(14),Color.rgb(220,235,252),true);text(c,"LOCAL PASS & PLAY",w/2,dp(39),dp(8),Color.rgb(116,143,174),true);
        float sw=dp(39),gap=dp(6),total=sw*3+gap*2,sx=(w-total)/2f,top=dp(47);text(c,"PLAYERS",sx-dp(27),top+dp(24),dp(8),Color.rgb(130,153,180),true);
        for(int i=0;i<3;i++){RectF r=new RectF(sx+i*(sw+gap),top,sx+i*(sw+gap)+sw,top+dp(27));playerSelectors[i]=r;fill(c,players==i+2?Color.rgb(52,83,119):Color.rgb(21,33,51),r.left,r.top,r.right,r.bottom,dp(9));stroke(c,players==i+2?Color.rgb(135,185,239):Color.rgb(50,68,91),dp(1),r.left,r.top,r.right,r.bottom,dp(9));text(c,String.valueOf(i+2),r.centerX(),r.centerY()+dp(5),dp(12),Color.WHITE,true);}
        float right=w-dp(12),y=dp(18);pauseRect.set(right-dp(29),y-dp(9),right,y+dp(19));soundRect.set(right-dp(66),y-dp(9),right-dp(36),y+dp(19));rulesRect.set(right-dp(103),y-dp(9),right-dp(73),y+dp(19));
        for(RectF r:new RectF[]{pauseRect,soundRect,rulesRect})fill(c,Color.rgb(25,41,61),r.left,r.top,r.right,r.bottom,dp(8));
        text(c,paused?"▶":"Ⅱ",pauseRect.centerX(),pauseRect.centerY()+dp(4),dp(12),Color.WHITE,true);text(c,sound?"♫":"♪",soundRect.centerX(),soundRect.centerY()+dp(4),dp(13),Color.WHITE,true);text(c,"?",rulesRect.centerX(),rulesRect.centerY()+dp(4),dp(13),Color.WHITE,true);
    }
    private void drawBoard(Canvas c){float x=boardX,y=boardY,s=boardSize;fill(c,Color.rgb(22,36,55),x-dp(3),y-dp(3),x+s+dp(3),y+s+dp(3),dp(15));stroke(c,Color.rgb(82,111,145),dp(1),x-dp(3),y-dp(3),x+s+dp(3),y+s+dp(3),dp(15));
        int[][] zone={{0,0},{0,9},{9,9},{9,0}};
        for(int i=0;i<4;i++){float l=x+zone[i][1]*cell,t=y+zone[i][0]*cell;fill(c,alpha(colors[i],95),l+cell*.08f,t+cell*.08f,l+cell*5.92f,t+cell*5.92f,cell*.28f);fill(c,Color.rgb(224,234,247),l+cell*.72f,t+cell*.72f,l+cell*5.28f,t+cell*5.28f,cell*.38f);
            for(int j=0;j<4;j++){float cx=l+cell*(1.8f+(j%2)*2.4f),cy=t+cell*(1.8f+(j/2)*2.4f);p.setColor(alpha(colors[i],65));c.drawCircle(cx,cy,cell*.68f,p);p.setColor(colors[i]);c.drawCircle(cx,cy,cell*.47f,p);p.setColor(Color.WHITE);c.drawCircle(cx-cell*.12f,cy-cell*.14f,cell*.12f,p);}}
        for(int i=0;i<52;i++){float[] q=xy(path[i][0],path[i][1]);int col=Color.rgb(231,239,248);if(i==0)col=colors[0];if(i==13)col=colors[1];if(i==26)col=colors[2];if(i==39)col=colors[3];fill(c,col,q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.07f);stroke(c,Color.rgb(172,192,214),dp(.5f),q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.07f);if(isSafe(i))text(c,"★",q[0],q[1]+cell*.23f,cell*.58f,Color.rgb(171,123,32),true);}
        for(int a=0;a<4;a++)for(int j=0;j<6;j++){float[] q=xy(lanes[a][j][0],lanes[a][j][1]);fill(c,alpha(colors[a],j==5?235:155),q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.06f);}
        Path tri=new Path();float cx=x+7.5f*cell,cy=y+7.5f*cell;
        tri.moveTo(x+6*cell,y+6*cell);tri.lineTo(x+9*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[1]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+6*cell);tri.lineTo(x+9*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[2]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+9*cell);tri.lineTo(x+6*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[3]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+6*cell,y+9*cell);tri.lineTo(x+6*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[0]);c.drawPath(tri,p);
        drawTokens(c);
    }
    private void drawTokens(Canvas c){for(int a=0;a<players;a++)for(int b=0;b<4;b++){float[] q=tokenPosition(a,b);int n=0,index=0;for(int k=0;k<players;k++)for(int j=0;j<4;j++){if(k==a&&j==b)index=n;if(tokens[k][j]>=0&&tokens[k][j]<57){float[] z=tokenPosition(k,j);if(Math.abs(z[0]-q[0])<1&&Math.abs(z[1]-q[1])<1)n++;}}
            float ox=0,oy=0;if(n>1){int cols=2;ox=((index%cols)-.5f)*cell*.34f;oy=((index/cols)-.5f)*cell*.34f;}
            float r=cell*(n>1?.23f:.34f)*((animating&&a==turn&&b==movingPiece)?animatedScale:1f);float tx=q[0]+ox,ty=q[1]+oy;
            p.setColor(alpha(Color.BLACK,110));c.drawCircle(tx+dp(1),ty+dp(2),r*1.12f,p);p.setColor(colors[a]);c.drawCircle(tx,ty,r,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));c.drawCircle(tx,ty,r,p);p.setStyle(Paint.Style.FILL);p.setColor(alpha(Color.WHITE,180));c.drawCircle(tx-r*.25f,ty-r*.28f,r*.22f,p);
            if(a==turn&&dice>0&&legal(a).contains(b)){p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));c.drawCircle(tx,ty,r*1.3f,p);p.setStyle(Paint.Style.FILL);}
        }}
    private int movingPiece=-1;
    private void drawHud(Canvas c,float w,float hh,float gap){float margin=dp(9),top=dp(80),bottom=getHeight()-dp(12);float left=margin,right=w-margin,boardTop=boardY,boardBottom=boardY+boardSize;
        RectF[] rs={new RectF(left,top,left+(w-3*margin)/2,Math.min(boardTop-dp(5),top+hh)),new RectF(right-(w-3*margin)/2,top,right,Math.min(boardTop-dp(5),top+hh)),new RectF(left,Math.max(boardBottom+dp(5),bottom-hh),left+(w-3*margin)/2,bottom),new RectF(right-(w-3*margin)/2,Math.max(boardBottom+dp(5),bottom-hh),right,bottom)};
        for(int i=0;i<4;i++){huds[i]=rs[i];RectF r=rs[i];boolean active=i==turn;fill(c,active?Color.rgb(31,49,72):Color.rgb(17,29,46),r.left,r.top,r.right,r.bottom,dp(12));stroke(c,active?colors[i]:Color.rgb(51,70,94),active?dp(2):dp(1),r.left,r.top,r.right,r.bottom,dp(12));float pad=dp(9);text(c,names[i],r.left+pad+dp(20),r.top+dp(19),dp(10),colors[i],true);text(c,(i==turn?seconds+"s":""),r.right-pad-dp(16),r.top+dp(19),dp(9),Color.WHITE,true);
            text(c,"HOME "+homeCount(i)+"/4",r.left+pad+dp(18),r.top+dp(36),dp(8),Color.rgb(178,197,218),false);text(c,"CAP "+captures[i]+"  ·  6s "+sixRolls[i],r.left+pad+dp(50),r.top+dp(53),dp(8),Color.rgb(178,197,218),false);
            RectF d=new RectF(r.right-dp(42),r.top+dp(8),r.right-dp(7),r.bottom-dp(8));rollRects[i]=d;fill(c,active?colors[i]:Color.rgb(47,65,87),d.left,d.top,d.right,d.bottom,dp(9));text(c,active&&dice>0?dieGlyph(dice):"⚄",d.centerX(),d.centerY()+dp(6),dp(20),Color.WHITE,true);if(active)text(c,"ROLL",d.centerX(),d.bottom-dp(2),dp(7),Color.WHITE,true);
        }
        text(c,message,w/2,boardY+boardSize+dp(15),dp(9),Color.rgb(169,191,216),true);
    }
    private int homeCount(int player){int n=0;for(int v:tokens[player])if(v==57)n++;return n;}
    private String dieGlyph(int n){return new String[]{"","⚀","⚁","⚂","⚃","⚄","⚅"}[n];}
    private float[] xy(int row,int col){return new float[]{boardX+(col+.5f)*cell,boardY+(row+.5f)*cell};}
    private boolean isSafe(int v){for(int a:safe)if(a==v)return true;return false;}
    private int global(int player,int progress){return(starts[player]+progress)%52;}
    private float[] tokenPosition(int player,int piece){int v=tokens[player][piece];if(v<0){int[][] b=bases[player];return xy(b[piece][0],b[piece][1]);}if(v==57)return xy(7,7);if(v>=52)return xy(lanes[player][Math.min(5,v-52)][0],lanes[player][Math.min(5,v-52)][1]);int g=global(player,v);return xy(path[g][0],path[g][1]);}
    private ArrayList<Integer> legal(int player){ArrayList<Integer> a=new ArrayList<>();for(int j=0;j<4;j++){int v=tokens[player][j];if(v==57)continue;if(v<0?dice==6:v+dice<=57)a.add(j);}return a;}
    private void rollDice(){if(paused||winner||animating||dice!=0)return;int n=1+random.nextInt(6);dice=n;if(n==6){sixRolls[turn]++;sixChain++;}else sixChain=0;if(sixChain>=3){dice=0;sixChain=0;message="Three sixes — turn forfeited";nextTurn();return;}if(legal(turn).isEmpty()){message="No legal move";if(n!=6)nextTurn();else dice=0;}}
    private void playStepSound(){if(sound&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,35);}catch(Exception ignored){}}
    private void moveToken(final int piece){if(dice==0||animating||!legal(turn).contains(piece))return;final int player=turn,pips=dice;dice=0;animating=true;movingPiece=piece;int old=tokens[player][piece];final int steps=old<0?1:pips;final int start=old<0?-1:old;
        runStep(player,piece,start,steps,0,pips);
    }
    private void runStep(final int player,final int piece,final int start,final int total,final int index,final int pips){
        if(index>=total){finishMove(player,piece,pips);return;}
        int next=start<0?0:start+index+1;tokens[player][piece]=next;
        ValueAnimator animator=ValueAnimator.ofFloat(.88f,1.16f,1f);animator.setDuration(145);animator.setInterpolator(new AccelerateDecelerateInterpolator());animator.addUpdateListener(a->{animatedScale=(float)a.getAnimatedValue();invalidate();});animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){playStepSound();runStep(player,piece,start,total,index+1,pips);}});animator.start();
    }
    private void finishMove(int player,int piece,int pips){int dest=tokens[player][piece];boolean captured=false;if(dest<52){int square=global(player,dest);if(!isSafe(square)){for(int q=0;q<players;q++)if(q!=player)for(int j=0;j<4;j++){int v=tokens[q][j];if(v>=0&&v<52&&global(q,v)==square){tokens[q][j]=-1;captures[player]++;captured=true;}}}}
        boolean won=true;for(int v:tokens[player])if(v!=57)won=false;animating=false;movingPiece=-1;animatedScale=1f;
        if(won){winner=true;message=names[player]+" WINS!";handler.removeCallbacks(countdown);}else if(pips==6||captured){turn=player;seconds=15;message=captured?"Captured! Roll again":"Six! Roll again";}else{message="";nextTurn();}invalidate();
    }
    private void nextTurn(){dice=0;sixChain=0;turn=(turn+1)%players;seconds=15;handler.removeCallbacks(countdown);if(!paused&&!winner)handler.postDelayed(countdown,1000);}
    private void drawOverlay(Canvas c,float w,float h){fill(c,alpha(Color.BLACK,205),0,0,w,h,0);float l=dp(25),r=w-dp(25),t=h/2-dp(78),b=h/2+dp(78);fill(c,Color.rgb(24,39,60),l,t,r,b,dp(18));stroke(c,Color.rgb(103,142,184),dp(1),l,t,r,b,dp(18));text(c,winner?"🏆  "+names[turn]+" WINS":"GAME PAUSED",w/2,t+dp(42),dp(20),Color.WHITE,true);text(c,winner?"All four tokens reached home":"The turn timer is paused",w/2,t+dp(70),dp(11),Color.rgb(184,205,229),false);text(c,winner?"TAP TO PLAY AGAIN":"TAP RESUME TO CONTINUE",w/2,t+dp(111),dp(10),Color.rgb(139,190,244),true);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();if(winner){reset();return true;}if(paused){paused=false;message="Game resumed";seconds=15;handler.postDelayed(countdown,1000);invalidate();return true;}
        for(int i=0;i<3;i++)if(playerSelectors[i]!=null&&playerSelectors[i].contains(x,y)){players=i+2;reset();return true;}
        if(pauseRect.contains(x,y)){paused=true;handler.removeCallbacks(countdown);invalidate();return true;}if(soundRect.contains(x,y)){sound=!sound;invalidate();return true;}if(rulesRect.contains(x,y)){message="6 to exit base · exact roll home · stars are safe";invalidate();return true;}
        if(animating)return true;
        for(int i=0;i<players;i++)if(rollRects[i]!=null&&rollRects[i].contains(x,y)&&i==turn){if(dice==0)rollDice();else{ArrayList<Integer> options=legal(turn);if(options.size()==1)moveToken(options.get(0));else message="Tap a highlighted token";}invalidate();return true;}
        if(dice>0){for(int j=0;j<4;j++){float[] q=tokenPosition(turn,j);if(legal(turn).contains(j)&&Math.hypot(x-q[0],y-q[1])<cell*.82f){moveToken(j);invalidate();return true;}}}
        invalidate();return true;
    }
}
