package com.ludoglassarena;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Camera;
import android.graphics.Matrix;
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
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LudoView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final Camera diceCamera=new Camera(); private final Matrix diceMatrix=new Matrix();
    private final Random random=new Random();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final int[] colors={Color.rgb(225,29,72),Color.rgb(16,185,129),Color.rgb(59,130,246),Color.rgb(245,158,11)};
    private final int[] colorDeep={Color.rgb(190,18,60),Color.rgb(4,120,87),Color.rgb(29,78,216),Color.rgb(180,83,9)};
    private final String[] names={"RUBY","JADE","SAPPHIRE","TOPAZ"};
    private final int[][] path={{6,1},{6,2},{6,3},{6,4},{6,5},{5,6},{4,6},{3,6},{2,6},{1,6},{0,6},{0,7},{0,8},{1,8},{2,8},{3,8},{4,8},{5,8},{6,9},{6,10},{6,11},{6,12},{6,13},{6,14},{7,14},{8,14},{8,13},{8,12},{8,11},{8,10},{8,9},{9,8},{10,8},{11,8},{12,8},{13,8},{14,8},{14,7},{14,6},{13,6},{12,6},{11,6},{10,6},{9,6},{8,5},{8,4},{8,3},{8,2},{8,1},{8,0},{7,0},{6,0}};
    private final int[][][] bases={{{1,1},{1,4},{4,1},{4,4}},{{1,10},{1,13},{4,10},{4,13}},{{10,10},{10,13},{13,10},{13,13}},{{10,1},{10,4},{13,1},{13,4}}};
    private final int[][][] lanes={{{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}},{{1,7},{2,7},{3,7},{4,7},{5,7},{6,7}},{{7,13},{7,12},{7,11},{7,10},{7,9},{7,8}},{{13,7},{12,7},{11,7},{10,7},{9,7},{8,7}}};
    private final int[] starts={0,13,26,39},safe={0,8,13,21,26,34,39,47};
    // progress: -1 base, 0..51 outer track, 52..56 home lane, 57 finished.
    private final int[][] tokens=new int[4][4];
    private final int[] captures=new int[4],sixRolls=new int[4];
    private int players=4,turn=0,dice=0,sixChain=0;
    private boolean paused=false,winner=false,sound=true,animating=false;
    private String message="Roll a six to leave your base";
    private float boardX,boardY,boardSize,cell,headerBottom;
    private final RectF[] huds=new RectF[4],rollRects=new RectF[4],playerSelectors=new RectF[3];
    private RectF pauseRect=new RectF(),soundRect=new RectF(),rulesRect=new RectF();
    private float animatedScale=1f,diceRotation=0f,moveOffsetX=0f,moveOffsetY=0f,dicePulse=1f; private boolean rolling=false,showRules=false; private final ArrayList<Particle> particles=new ArrayList<>(); private long lastFrame=0; private int selectedPiece=-1,pressedControl=-1; private final long[] trailExpiry=new long[52]; private final int[] trailOwner=new int[52]; private ValueAnimator pulseAnimator;
    private ToneGenerator tone; private static class Particle { float x,y,vx,vy,life,size; int color; Particle(float x,float y,float vx,float vy,float life,float size,int color){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.size=size;this.color=color;} }
    public LudoView(Context context){super(context);setLayerType(View.LAYER_TYPE_HARDWARE,null);setWillNotDraw(false);try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,35);}catch(Exception ignored){}reset();}

    private void drawAmbientBackground(Canvas c,float w,float h){
        c.drawColor(Color.rgb(10,16,29));
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new RadialGradient(w*.12f,h*.18f,Math.max(w,h)*.72f,new int[]{0x332D5B9A,0x001D4ED8,0x00000000},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,bg);bg.setShader(new RadialGradient(w*.92f,h*.76f,Math.max(w,h)*.62f,new int[]{0x222E176B,0x001D4ED8,0x00000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,bg);
        p.setColor(0x16FFFFFF);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(dp(7),dp(7),w-dp(7),h-dp(7),dp(22),dp(22),p);p.setStyle(Paint.Style.FILL);
    }
    private void spawnCaptureBurst(float x,float y,int color){
        for(int i=0;i<14;i++){double a=(Math.PI*2*i)/14.0+random.nextDouble()*.22;float speed=dp(1.8f+(random.nextFloat()*2.6f));particles.add(new Particle(x,y,(float)Math.cos(a)*speed,(float)Math.sin(a)*speed,1f,.8f+random.nextFloat()*1.5f,color));}
        invalidate();
    }
    private void drawParticles(Canvas c){
        if(particles.isEmpty())return;long now=System.nanoTime();float dt=lastFrame==0?.016f:Math.min(.04f,(now-lastFrame)/1_000_000_000f);lastFrame=now;
        for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.x+=q.vx;q.y+=q.vy;q.vy+=dp(.12f);q.life-=dt*2.2f;if(q.life<=0){particles.remove(i);continue;}p.setColor(Color.argb((int)(Math.max(0,q.life)*220),Color.red(q.color),Color.green(q.color),Color.blue(q.color)));Path shard=new Path();shard.moveTo(q.x,q.y);shard.lineTo(q.x+q.size*dp(3),q.y-q.size*dp(1));shard.lineTo(q.x+q.size*dp(1),q.y+q.size*dp(3));shard.close();c.drawPath(shard,p);}
        if(!particles.isEmpty())postInvalidateOnAnimation();
    }
    private void triggerHaptic(boolean strong){
        try{if(Build.VERSION.SDK_INT>=31){Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createPredefined(strong?VibrationEffect.EFFECT_HEAVY_CLICK:VibrationEffect.EFFECT_TICK));}else if(Build.VERSION.SDK_INT>=26){Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.vibrate(VibrationEffect.createOneShot(strong?35:12,strong?110:55));}}catch(Exception ignored){}
    }

    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    public void reset(){for(int a=0;a<4;a++){captures[a]=0;sixRolls[a]=0;for(int b=0;b<4;b++)tokens[a][b]=-1;}java.util.Arrays.fill(trailExpiry,0L);java.util.Arrays.fill(trailOwner,-1);turn=0;dice=0;sixChain=0;selectedPiece=-1;paused=false;winner=false;animating=false;rolling=false;message="Roll the die to begin";invalidate();}
    private void fill(Canvas c,int color,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,rad,rad,p);}
    private void stroke(Canvas c,int color,float width,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawRoundRect(l,t,r,b,rad,rad,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
    private int alpha(int color,int a){return Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();drawAmbientBackground(c,w,h);
        drawHeader(c,w);
        float hudH=dp(77),gap=dp(9),available=Math.max(dp(190),h-dp(35)-hudH*2-gap*2);
        boardSize=Math.min(w-dp(18),available);cell=boardSize/15f;boardX=(w-boardSize)/2f;boardY=(h-boardSize)/2f+dp(4);
        drawBoard(c);
        drawHud(c,w,hudH,gap);
        if(paused||winner||showRules)drawOverlay(c,w,h);
    }
    private void drawHeader(Canvas c,float w){text(c,"LUDO",w/2-dp(28),dp(25),dp(17),Color.rgb(248,250,252),true);text(c,"GLASS ARENA",w/2+dp(29),dp(25),dp(9),Color.rgb(148,163,184),true);text(c,"PRIVATE TABLE  ·  PASS & PLAY",w/2,dp(40),dp(7),Color.rgb(100,116,139),true);
        float sw=dp(39),gap=dp(6),total=sw*3+gap*2,sx=(w-total)/2f,top=dp(47);text(c,"PLAYERS",sx-dp(27),top+dp(24),dp(8),Color.rgb(130,153,180),true);
        for(int i=0;i<3;i++){RectF r=new RectF(sx+i*(sw+gap),top,sx+i*(sw+gap)+sw,top+dp(27));playerSelectors[i]=r;fill(c,players==i+2?Color.rgb(52,83,119):Color.rgb(21,33,51),r.left,r.top,r.right,r.bottom,dp(9));stroke(c,players==i+2?Color.rgb(135,185,239):Color.rgb(50,68,91),dp(1),r.left,r.top,r.right,r.bottom,dp(9));text(c,String.valueOf(i+2),r.centerX(),r.centerY()+dp(5),dp(12),Color.WHITE,true);}
        float right=w-dp(12),y=dp(18);pauseRect.set(right-dp(29),y-dp(9),right,y+dp(19));soundRect.set(right-dp(66),y-dp(9),right-dp(36),y+dp(19));rulesRect.set(right-dp(103),y-dp(9),right-dp(73),y+dp(19));
        for(int ci=0;ci<3;ci++){RectF r=new RectF[]{pauseRect,soundRect,rulesRect}[ci];c.save();if(pressedControl==ci)c.scale(.94f,.94f,r.centerX(),r.centerY());fill(c,Color.argb(190,24,39,60),r.left,r.top,r.right,r.bottom,dp(9));stroke(c,Color.argb(55,255,255,255),dp(1),r.left,r.top,r.right,r.bottom,dp(9));c.restore();}
        text(c,paused?"▶":"Ⅱ",pauseRect.centerX(),pauseRect.centerY()+dp(4),dp(11),Color.WHITE,true);text(c,sound?"◖":"◗",soundRect.centerX(),soundRect.centerY()+dp(4),dp(12),Color.WHITE,true);text(c,"☰",rulesRect.centerX(),rulesRect.centerY()+dp(4),dp(11),Color.WHITE,true);
    }
    private void drawBoard(Canvas c){float x=boardX,y=boardY,s=boardSize;Paint boardPaint=new Paint(Paint.ANTI_ALIAS_FLAG);boardPaint.setColor(Color.BLACK);boardPaint.setShadowLayer(dp(18),0,dp(10),0xAA000000);c.drawRoundRect(x-dp(5),y-dp(3),x+s+dp(5),y+s+dp(7),dp(17),dp(17),boardPaint);boardPaint.clearShadowLayer();boardPaint.setShader(new LinearGradient(x,y,x+s,y+s,Color.rgb(54,72,96),Color.rgb(13,23,39),Shader.TileMode.CLAMP));c.drawRoundRect(x-dp(4),y-dp(4),x+s+dp(4),y+s+dp(4),dp(16),dp(16),boardPaint);boardPaint.setShader(null);stroke(c,Color.argb(105,255,255,255),dp(1.3f),x-dp(4),y-dp(4),x+s+dp(4),y+s+dp(4),dp(16));stroke(c,Color.argb(150,0,0,0),dp(1),x,y,x+s,y+s,dp(12));
        int[][] zone={{0,0},{0,9},{9,9},{9,0}};
        for(int i=0;i<4;i++){float l=x+zone[i][1]*cell,t=y+zone[i][0]*cell;fill(c,alpha(colors[i],i==turn?135:62),l+cell*.08f,t+cell*.08f,l+cell*5.92f,t+cell*5.92f,cell*.28f);if(i==turn){p.setColor(alpha(colors[i],48));p.setShadowLayer(dp(20),0,0,colors[i]);c.drawRoundRect(l+cell*.2f,t+cell*.2f,l+cell*5.8f,t+cell*5.8f,cell*.32f,cell*.32f,p);p.clearShadowLayer();}fill(c,Color.rgb(224,234,247),l+cell*.72f,t+cell*.72f,l+cell*5.28f,t+cell*5.28f,cell*.38f);
            if(i==turn){p.setColor(alpha(colors[i],55));p.setShadowLayer(dp(12),0,0,colors[i]);c.drawCircle(l+cell*3,t+cell*3,cell*2.45f,p);p.clearShadowLayer();}for(int j=0;j<4;j++){float cx=l+cell*(1.8f+(j%2)*2.4f),cy=t+cell*(1.8f+(j/2)*2.4f);p.setColor(alpha(colors[i],65));c.drawCircle(cx,cy,cell*.68f,p);p.setColor(colors[i]);c.drawCircle(cx,cy,cell*.47f,p);p.setColor(Color.WHITE);c.drawCircle(cx-cell*.12f,cy-cell*.14f,cell*.12f,p);}}
        for(int i=0;i<52;i++){float[] q=xy(path[i][0],path[i][1]);int col=Color.rgb(232,237,244);if(i==0)col=colors[0];if(i==13)col=colors[1];if(i==26)col=colors[2];if(i==39)col=colors[3];p.setShader(new LinearGradient(q[0]-cell*.4f,q[1]-cell*.4f,q[0]+cell*.4f,q[1]+cell*.4f,Color.rgb(250,252,255),col==Color.rgb(232,237,244)?Color.rgb(205,216,229):col,Shader.TileMode.CLAMP));c.drawRoundRect(q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.07f,cell*.07f,p);p.setShader(null);stroke(c,Color.argb(65,255,255,255),dp(.5f),q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.07f);stroke(c,Color.argb(110,0,0,0),dp(.5f),q[0]-cell*.43f,q[1]-cell*.43f,q[0]+cell*.43f,q[1]+cell*.43f,cell*.05f);if(isSafe(i)){text(c,"★",q[0]+dp(.6f),q[1]+cell*.25f+dp(1),cell*.56f,Color.argb(130,15,23,42),true);text(c,"★",q[0],q[1]+cell*.22f,cell*.56f,Color.rgb(197,156,76),true);text(c,"★",q[0]-dp(.5f),q[1]+cell*.20f,cell*.48f,Color.rgb(255,231,173),true);}}
        for(int a=0;a<4;a++)for(int j=0;j<6;j++){float[] q=xy(lanes[a][j][0],lanes[a][j][1]);p.setShader(new LinearGradient(q[0]-cell*.4f,q[1]-cell*.4f,q[0]+cell*.4f,q[1]+cell*.4f,alpha(colors[a],j==5?245:205),alpha(colorDeep[a],j==5?245:170),Shader.TileMode.CLAMP));c.drawRoundRect(q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.06f,cell*.06f,p);p.setShader(null);stroke(c,Color.argb(80,255,255,255),dp(.6f),q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.06f);}
        Path tri=new Path();float cx=x+7.5f*cell,cy=y+7.5f*cell;
        tri.moveTo(x+6*cell,y+6*cell);tri.lineTo(x+9*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[1]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+6*cell);tri.lineTo(x+9*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[2]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+9*cell,y+9*cell);tri.lineTo(x+6*cell,y+9*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[3]);c.drawPath(tri,p);
        tri.reset();tri.moveTo(x+6*cell,y+9*cell);tri.lineTo(x+6*cell,y+6*cell);tri.lineTo(cx,cy);tri.close();p.setColor(colors[0]);c.drawPath(tri,p);
        p.setShader(new RadialGradient(cx-cell*.18f,cy-cell*.22f,cell*1.25f,new int[]{Color.WHITE,Color.rgb(180,205,235),Color.rgb(45,63,91)},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,cell*.56f,p);p.setShader(null);p.setColor(Color.argb(180,255,255,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawCircle(cx,cy,cell*.56f,p);p.setStyle(Paint.Style.FILL);
        drawPathHighlights(c);drawTokens(c);drawParticles(c);
    }
    private void drawTokens(Canvas c){for(int a=0;a<players;a++)for(int b=0;b<4;b++){float[] q=tokenPosition(a,b);int n=0,index=0;for(int k=0;k<players;k++)for(int j=0;j<4;j++){if(k==a&&j==b)index=n;if(tokens[k][j]>=0&&tokens[k][j]<57){float[] z=tokenPosition(k,j);if(Math.abs(z[0]-q[0])<1&&Math.abs(z[1]-q[1])<1)n++;}}
            float ox=0,oy=0;if(n>1){int cols=2;ox=((index%cols)-.5f)*cell*.34f;oy=((index/cols)-.5f)*cell*.34f;}
            float r=cell*(n>1?.255f:.34f)*((animating&&a==turn&&b==movingPiece)?animatedScale:1f);float tx=q[0]+ox,ty=q[1]+oy;if(animating&&a==turn&&b==movingPiece){tx+=moveOffsetX;ty+=moveOffsetY;}
            p.setColor(alpha(Color.BLACK,165));p.setShadowLayer(dp(4),dp(1),dp(3),0xCC000000);c.drawOval(tx-r*.92f,ty-r*.45f,tx+r*.92f,ty+r*.82f,p);p.clearShadowLayer();p.setShader(new RadialGradient(tx-r*.34f,ty-r*.42f,r*1.55f,new int[]{Color.rgb(255,255,255),colors[a],colorDeep[a],Color.rgb(10,18,32)},new float[]{0f,.22f,.78f,1f},Shader.TileMode.CLAMP));c.drawCircle(tx,ty,r,p);p.setShader(null);p.setColor(Color.argb(125,255,255,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawCircle(tx,ty,r,p);p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(tx-r*.36f,ty-r*.42f,r*.72f,new int[]{Color.argb(245,255,255,255),Color.argb(95,255,255,255),Color.argb(0,255,255,255)},null,Shader.TileMode.CLAMP));c.drawCircle(tx-r*.20f,ty-r*.25f,r*.48f,p);p.setShader(null);p.setColor(alpha(colorDeep[a],100));c.drawCircle(tx+r*.23f,ty+r*.32f,r*.22f,p);
            if(a==turn&&dice>0&&legal(a).contains(b)){p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));c.drawCircle(tx,ty,r*1.3f,p);p.setStyle(Paint.Style.FILL);}
        }}
    private int movingPiece=-1;
    private void drawHud(Canvas c,float w,float hh,float gap){if(pulseAnimator==null){pulseAnimator=ValueAnimator.ofFloat(1f,1.045f);pulseAnimator.setDuration(1050);pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);pulseAnimator.addUpdateListener(a->{dicePulse=(float)a.getAnimatedValue();invalidate();});pulseAnimator.start();}float margin=dp(9),top=dp(80),bottom=getHeight()-dp(12);float left=margin,right=w-margin,boardTop=boardY,boardBottom=boardY+boardSize;
        RectF[] rs={new RectF(left,top,left+(w-3*margin)/2,Math.min(boardTop-dp(5),top+hh)),new RectF(right-(w-3*margin)/2,top,right,Math.min(boardTop-dp(5),top+hh)),new RectF(left,Math.max(boardBottom+dp(5),bottom-hh),left+(w-3*margin)/2,bottom),new RectF(right-(w-3*margin)/2,Math.max(boardBottom+dp(5),bottom-hh),right,bottom)};
        for(int slot=0;slot<4;slot++){int i=new int[]{0,1,3,2}[slot];huds[i]=rs[slot];RectF r=rs[slot];boolean active=i==turn;c.save();if(pressedControl==7&&active)c.scale(.975f,.975f,r.centerX(),r.centerY());fill(c,active?Color.rgb(28,43,64):Color.argb(205,15,27,44),r.left,r.top,r.right,r.bottom,dp(12));stroke(c,Color.argb(52,255,255,255),dp(1),r.left,r.top,r.right,r.bottom,dp(12));stroke(c,Color.argb(105,0,0,0),dp(1),r.left+dp(1),r.top+dp(1),r.right-dp(1),r.bottom-dp(1),dp(11));if(active)stroke(c,colors[i],dp(1.5f),r.left,r.top,r.right,r.bottom,dp(12));float pad=dp(9);if(active){p.setColor(alpha(colors[i],26));p.setShadowLayer(dp(15),0,0,colors[i]);c.drawRoundRect(r,dp(12),dp(12),p);p.clearShadowLayer();}p.setColor(colors[i]);c.drawCircle(r.left+pad+dp(9),r.top+dp(19),dp(9),p);p.setColor(alpha(Color.WHITE,220));c.drawCircle(r.left+pad+dp(6),r.top+dp(16),dp(2.5f),p);text(c,names[i],r.left+pad+dp(34),r.top+dp(17),dp(9),Color.rgb(241,245,249),true);text(c,(active?(dice>0?"YOUR MOVE":"YOUR TURN"):"READY"),r.right-pad-dp(24),r.top+dp(17),dp(6),active?colors[i]:Color.rgb(148,163,184),true);
            text(c,"HOME",r.left+pad+dp(17),r.top+dp(34),dp(7),Color.rgb(148,163,184),true);text(c,homeCount(i)+"/4",r.left+pad+dp(17),r.top+dp(48),dp(10),Color.rgb(248,250,252),true);text(c,"CAPTURES",r.left+pad+dp(58),r.top+dp(34),dp(7),Color.rgb(148,163,184),true);text(c,String.valueOf(captures[i]),r.left+pad+dp(58),r.top+dp(48),dp(10),Color.rgb(248,250,252),true);text(c,"SIXES",r.left+pad+dp(101),r.top+dp(34),dp(7),Color.rgb(148,163,184),true);text(c,String.valueOf(sixRolls[i]),r.left+pad+dp(101),r.top+dp(48),dp(10),Color.rgb(248,250,252),true);
            RectF d=new RectF(r.right-dp(42),r.top+dp(8),r.right-dp(7),r.bottom-dp(8));rollRects[i]=d;drawDie(c,d,active&&dice>0?dice:0,active);
        c.restore();}
        text(c,message,w/2,boardY+boardSize+dp(15),dp(9),Color.rgb(169,191,216),true);
    }
    private void drawPathHighlights(Canvas c){long now=System.currentTimeMillis();for(int i=0;i<52;i++){if(trailExpiry[i]<=now)continue;float f=Math.min(1f,(trailExpiry[i]-now)/1150f);float[] q=xy(path[i][0],path[i][1]);int owner=trailOwner[i];if(owner<0)continue;p.setColor(Color.argb((int)(75*f),Color.red(colors[owner]),Color.green(colors[owner]),Color.blue(colors[owner])));p.setShadowLayer(dp(7),0,0,colors[owner]);c.drawRoundRect(q[0]-cell*.47f,q[1]-cell*.47f,q[0]+cell*.47f,q[1]+cell*.47f,cell*.12f,cell*.12f,p);p.clearShadowLayer();postInvalidateOnAnimation();}if(dice>0&&!animating){for(int piece:legal(turn)){if(selectedPiece>=0&&piece!=selectedPiece)continue;int v=tokens[turn][piece],steps=v<0?1:dice;for(int k=0;k<steps;k++){int pos=v<0?k:v+k+1;float[] q;if(pos<52){int sq=global(turn,pos);q=xy(path[sq][0],path[sq][1]);}else if(pos<=56){q=xy(lanes[turn][pos-52][0],lanes[turn][pos-52][1]);}else continue;p.setColor(alpha(colors[turn],selectedPiece==piece?95:48));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(selectedPiece==piece?2.2f:1.2f));c.drawRoundRect(q[0]-cell*.43f,q[1]-cell*.43f,q[0]+cell*.43f,q[1]+cell*.43f,cell*.1f,cell*.1f,p);p.setStyle(Paint.Style.FILL);}}}}
    private int homeCount(int player){int n=0;for(int v:tokens[player])if(v==57)n++;return n;}
    private String dieGlyph(int n){return String.valueOf(n);}
    private void drawDie(Canvas c,RectF r,int value,boolean active){
        c.save();if(active&&rolling){diceCamera.save();diceCamera.rotateX(diceRotation*.91f);diceCamera.rotateY(diceRotation*1.13f);diceCamera.rotateZ(diceRotation*.24f);diceCamera.getMatrix(diceMatrix);diceCamera.restore();diceMatrix.preTranslate(-r.centerX(),-r.centerY());diceMatrix.postTranslate(r.centerX(),r.centerY());c.concat(diceMatrix);float bounce=(float)Math.abs(Math.sin(Math.toRadians(diceRotation*.5f)));c.translate(0,-dp(3)*bounce);c.scale(1f+.035f*bounce,1f+.035f*bounce,r.centerX(),r.centerY());}else if(!active){c.scale(.78f,.78f,r.centerX(),r.centerY());}else{c.scale(dicePulse,dicePulse,r.centerX(),r.centerY());}
        p.setStyle(Paint.Style.FILL);p.setColor(Color.BLACK);p.setShadowLayer(dp(5),0,dp(3),0x99000000);
        RectF d=new RectF(r.left+dp(2),r.top+dp(3),r.right-dp(2),r.bottom-dp(3));c.drawRoundRect(d,dp(8),dp(8),p);p.clearShadowLayer();
        p.setShader(new LinearGradient(d.left,d.top,d.right,d.bottom,active?Color.argb(245,255,255,255):Color.rgb(100,119,143),active?colors[turn]:Color.rgb(45,62,83),Shader.TileMode.CLAMP));c.drawRoundRect(d,dp(8),dp(8),p);p.setShader(null);
        p.setColor(0xCCFFFFFF);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));c.drawRoundRect(d,dp(8),dp(8),p);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(d.left,d.top,d.left,d.bottom,0x99FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));c.drawRoundRect(d,dp(8),dp(8),p);p.setShader(null);
        if(value==0){text(c,"?",d.centerX(),d.centerY()+dp(6),dp(19),Color.WHITE,true);}else{
            float rr=dp(2.4f),cx=d.centerX(),cy=d.centerY(),off=d.width()*.22f;
            int[][] dots={{},{4},{0,8},{0,4,8},{0,2,6,8},{0,2,4,6,8},{0,2,3,5,6,8}};
            for(int v:dots[value]){float dx=(v%3-1)*off,dy=(v/3-1)*off;p.setColor(Color.WHITE);c.drawCircle(cx+dx,cy+dy,rr,p);}
        }c.restore();
    }
    private float[] xy(int row,int col){return new float[]{boardX+(col+.5f)*cell,boardY+(row+.5f)*cell};}
    private boolean isSafe(int v){for(int a:safe)if(a==v)return true;return false;}
    private int global(int player,int progress){return(starts[player]+progress)%52;}
    private float[] tokenPosition(int player,int piece){int v=tokens[player][piece];if(v<0){int[][] b=bases[player];return xy(b[piece][0],b[piece][1]);}if(v==57)return xy(7,7);if(v>=52)return xy(lanes[player][Math.min(5,v-52)][0],lanes[player][Math.min(5,v-52)][1]);int g=global(player,v);return xy(path[g][0],path[g][1]);}
    private ArrayList<Integer> legal(int player){ArrayList<Integer> a=new ArrayList<>();for(int j=0;j<4;j++){int v=tokens[player][j];if(v==57)continue;if(v<0?dice==6:v+dice<=57)a.add(j);}return a;}
    private void rollDice(){if(paused||winner||animating||rolling||dice!=0)return;selectedPiece=-1;triggerHaptic(false);rolling=true;ValueAnimator tumble=ValueAnimator.ofFloat(0f,1080f);tumble.setDuration(820);tumble.setInterpolator(new android.view.animation.OvershootInterpolator(1.15f));tumble.addUpdateListener(a->{diceRotation=(float)a.getAnimatedValue();invalidate();});tumble.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){rolling=false;diceRotation=0;resolveRoll();}});tumble.start();}
    private void resolveRoll(){int n=1+random.nextInt(6);dice=n;playStepSound();if(n==6){sixRolls[turn]++;sixChain++;}else sixChain=0;if(sixChain>=3){dice=0;sixChain=0;message="Three sixes — turn forfeited";nextTurn();return;}if(legal(turn).isEmpty()){message="No legal move";if(n!=6)nextTurn();else dice=0;}else message=legal(turn).size()==1?"Tap the highlighted token":"Choose a token to move";invalidate();}
    private void playStepSound(){if(sound&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,28);}catch(Exception ignored){}}
    private void moveToken(final int piece){if(dice==0||animating||!legal(turn).contains(piece))return;final int player=turn,pips=dice;dice=0;selectedPiece=-1;animating=true;movingPiece=piece;int old=tokens[player][piece];final int steps=old<0?1:pips;final int start=old<0?-1:old;
        runStep(player,piece,start,steps,0,pips);
    }
    private void runStep(final int player,final int piece,final int start,final int total,final int index,final int pips){
        if(index>=total){finishMove(player,piece,pips);return;}
        float[] from=tokenPosition(player,piece);int next=start<0?0:start+index+1;tokens[player][piece]=next;float[] to=tokenPosition(player,piece);if(next>=0&&next<52){int square=global(player,next);trailOwner[square]=player;trailExpiry[square]=System.currentTimeMillis()+1150;}final float dx=from[0]-to[0],dy=from[1]-to[1];moveOffsetX=dx;moveOffsetY=dy;ValueAnimator animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(205);animator.setInterpolator(new android.view.animation.PathInterpolator(.34f,1.45f,.64f,1f));animator.addUpdateListener(a->{float f=(float)a.getAnimatedValue();moveOffsetX=dx*(1f-f);moveOffsetY=dy*(1f-f)-(float)Math.sin(Math.PI*f)*cell*.58f;animatedScale=f<.82f?1f+.06f*(float)Math.sin(Math.PI*f):1f-.12f*(1f-f)/.18f;invalidate();});animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){moveOffsetX=0;moveOffsetY=0;animatedScale=1f;playStepSound();runStep(player,piece,start,total,index+1,pips);}});animator.start();
    }
    private void finishMove(int player,int piece,int pips){int dest=tokens[player][piece];boolean captured=false;if(dest<52){int square=global(player,dest);if(!isSafe(square)){for(int q=0;q<players;q++)if(q!=player)for(int j=0;j<4;j++){int v=tokens[q][j];if(v>=0&&v<52&&global(q,v)==square){tokens[q][j]=-1;captures[player]++;captured=true;spawnCaptureBurst(tokenPosition(player,piece)[0],tokenPosition(player,piece)[1],colors[q]);triggerHaptic(true);}}}}
        boolean won=true;for(int v:tokens[player])if(v!=57)won=false;animating=false;movingPiece=-1;animatedScale=1f;moveOffsetX=0;moveOffsetY=0;
        if(won){winner=true;message=names[player]+" WINS!";triggerHaptic(true);}else if(pips==6||captured){turn=player;message=captured?"Captured! Roll again":"Six! Roll again";}else{message="";nextTurn();}invalidate();
    }
    private void nextTurn(){dice=0;sixChain=0;selectedPiece=-1;turn=(turn+1)%players;message=names[turn]+" to play";invalidate();}
    private void drawOverlay(Canvas c,float w,float h){
        fill(c,alpha(Color.BLACK,215),0,0,w,h,0);float l=dp(22),r=w-dp(22),t=h/2-dp(showRules?145:82),b=h/2+dp(showRules?145:82);
        fill(c,Color.rgb(20,34,54),l,t,r,b,dp(20));stroke(c,Color.rgb(113,157,205),dp(1.3f),l,t,r,b,dp(20));
        if(showRules){
            text(c,"HOW TO PLAY",w/2,t+dp(31),dp(18),Color.WHITE,true);
            String[] lines={"Roll a 6 to release a token from base","Move clockwise; exact roll to reach home","A 6 grants another roll","Capture opponents on non-safe squares","Star squares are safe from capture","Three consecutive 6s forfeit your turn","First player to bring all 4 tokens home wins","No turn timer — take your time"};
            for(int i=0;i<lines.length;i++)text(c,lines[i],w/2,t+dp(62+i*dp(21)),dp(9),Color.rgb(197,215,237),false);
            text(c,"TAP ANYWHERE TO CLOSE",w/2,b-dp(19),dp(9),Color.rgb(139,190,244),true);return;
        }
        text(c,winner?"✦  "+names[turn]+" WINS":"GAME PAUSED",w/2,t+dp(43),dp(20),Color.WHITE,true);
        text(c,winner?"All four tokens reached home":"Take a moment. The table is paused.",w/2,t+dp(73),dp(11),Color.rgb(184,205,229),false);
        text(c,winner?"TAP TO PLAY AGAIN":"TAP RESUME TO CONTINUE",w/2,t+dp(119),dp(10),Color.rgb(139,190,244),true);
    }
    private int hitControl(float x,float y){if(pauseRect.contains(x,y))return 0;if(soundRect.contains(x,y))return 1;if(rulesRect.contains(x,y))return 2;for(int i=0;i<3;i++)if(playerSelectors[i]!=null&&playerSelectors[i].contains(x,y))return 3+i;for(int i=0;i<players;i++)if(i==turn&&rollRects[i]!=null&&rollRects[i].contains(x,y))return 7;return -1;}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){pressedControl=hitControl(e.getX(),e.getY());invalidate();return true;}if(e.getAction()!=MotionEvent.ACTION_UP)return true;pressedControl=-1;float x=e.getX(),y=e.getY();if(winner){reset();return true;}if(showRules){showRules=false;invalidate();return true;}if(paused){paused=false;message="Game resumed";invalidate();return true;}
        for(int i=0;i<3;i++)if(playerSelectors[i]!=null&&playerSelectors[i].contains(x,y)){players=i+2;reset();return true;}
        if(pauseRect.contains(x,y)){paused=true;invalidate();return true;}if(soundRect.contains(x,y)){sound=!sound;invalidate();return true;}if(rulesRect.contains(x,y)){showRules=true;invalidate();return true;}
        if(animating||rolling)return true;
        for(int i=0;i<players;i++)if(rollRects[i]!=null&&rollRects[i].contains(x,y)&&i==turn){if(dice==0)rollDice();else if(selectedPiece>=0)moveToken(selectedPiece);invalidate();return true;}
        if(dice>0){for(int j=0;j<4;j++){float[] q=tokenPosition(turn,j);if(legal(turn).contains(j)&&Math.hypot(x-q[0],y-q[1])<cell*.9f){if(selectedPiece==j)moveToken(j);else{selectedPiece=j;message="Path preview — tap token again to move";}invalidate();return true;}}}
        invalidate();return true;
    }
}
