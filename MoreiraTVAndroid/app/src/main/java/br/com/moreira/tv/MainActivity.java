package br.com.moreira.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.text.InputType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG=0xFF080E1B, SURFACE=0xFF121E32, BORDER=0xFF283955, WHITE=0xFFF4F8FF, MUTED=0xFF9CB1CE, ACCENT=0xFF33D5DB, BLUE=0xFF86B4FF;
    private static final String DEFAULT_SERVER="http://aurinhatv.com";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private EditText hostInput, userInput, passInput, searchInput;
    private TextView status, title;
    private LinearLayout authCard, menu;
    private Button[] tabs=new Button[3];
    private Spinner kindSpinner, categorySpinner;
    private ListView list;
    private Button connect;
    private String server, username, password;
    private boolean loggedIn=false, changing=false;
    private int generation=0;
    private final List<JSONObject> allItems=new ArrayList<>(), shownItems=new ArrayList<>();
    private final List<String> categories=new ArrayList<>(), categoryIds=new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private final List<String> shownNames=new ArrayList<>();
    private String type="live";


    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        buildUi();
        String[] access=CredentialsVault.load(this);
        if(access!=null){
            hostInput.setText(access[0]);userInput.setText(access[1]);passInput.setText(access[2]);
            authCard.setVisibility(View.GONE);setStatus("Preparando sua programação…",false);
            login();
        }
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int fill,int stroke,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        v.setPadding(dp(2),dp(5),dp(2),dp(5));
        if(bold)v.setTypeface(null,Typeface.BOLD);return v;
    }
    private EditText input(String hint,int inputType){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(0xFF8197B6);
        e.setTextColor(WHITE);e.setTextSize(15);e.setSingleLine(true);e.setInputType(inputType);
        e.setPadding(dp(14),dp(11),dp(14),dp(11));e.setBackground(shape(SURFACE,BORDER,12));
        return e;
    }
    private LinearLayout root(){
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(15),dp(14),dp(15),dp(8));v.setBackgroundColor(BG);return v;
    }
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private void gap(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private Button button(String text,boolean active){
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(13);
        b.setTypeface(null,Typeface.BOLD);b.setTextColor(active?BG:WHITE);
        b.setPadding(dp(6),dp(9),dp(6),dp(9));
        b.setBackground(shape(active?ACCENT:SURFACE,active?0:BORDER,13));return b;
    }
    private void updateTabs(){
        String[] kinds={"live","movie","series"};
        for(int i=0;i<3;i++){
            boolean active=kinds[i].equals(type);
            tabs[i].setTextColor(active?BG:WHITE);
            tabs[i].setBackground(shape(active?ACCENT:SURFACE,active?0:BORDER,13));
        }
    }
    private void showAccessMenu(){
        new AlertDialog.Builder(this).setTitle("Moreira TV — Acesso")
            .setItems(new String[]{"Configurar minha conexão","Apagar acesso salvo","Cancelar"},(d,n)->{
                if(n==0){authCard.setVisibility(View.VISIBLE);setStatus("Atualize seu acesso e toque em ENTRAR.",false);}
                else if(n==1){
                    new AlertDialog.Builder(this).setTitle("Apagar acesso deste celular?")
                        .setMessage("A operação não altera o serviço nem outros dispositivos.")
                        .setNegativeButton("Cancelar",null)
                        .setPositiveButton("Apagar",(a,b)->{
                            CredentialsVault.clear(this);loggedIn=false;username="";password="";
                            userInput.setText("");passInput.setText("");
                            menu.setVisibility(View.GONE);authCard.setVisibility(View.VISIBLE);
                            setStatus("Configure seu acesso pessoal.",false);
                        }).show();
                }
            }).show();
    }
    private void buildUi(){
        LinearLayout base=root();setContentView(base);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=label("▶",23,ACCENT,true);logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(SURFACE,BORDER,14));
        head.addView(logo,new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout names=vertical();names.setPadding(dp(12),0,0,0);
        title=label("MOREIRA TV",25,WHITE,true);names.addView(title);
        names.addView(label("SUA TV. SUA EXPERIÊNCIA.",10,ACCENT,true));
        head.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=button("⚙",false);settings.setContentDescription("Gerenciar acesso");
        settings.setOnClickListener(v->showAccessMenu());
        head.addView(settings,new LinearLayout.LayoutParams(dp(54),dp(50)));
        base.addView(head);gap(base,12);
        LinearLayout banner=vertical();banner.setPadding(dp(15),dp(12),dp(15),dp(13));
        GradientDrawable bkg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF17365C,0xFF11213A,0xFF17192E});bkg.setCornerRadius(dp(15));
        banner.setBackground(bkg);
        banner.addView(label("BEM-VINDO AO SEU ESPAÇO",11,ACCENT,true));
        banner.addView(label("O melhor da sua programação",18,WHITE,true));
        banner.addView(label("Escolha, descubra e assista em um só lugar.",12,MUTED,false));
        base.addView(banner);gap(base,11);
        authCard=vertical();authCard.setPadding(dp(14),dp(12),dp(14),dp(12));
        authCard.setBackground(shape(0xFF101D31,BORDER,16));base.addView(authCard);
        authCard.addView(label("ATIVAR MINHA TV",17,WHITE,true));
        authCard.addView(label("Configure uma vez. Depois, entre automaticamente.",12,MUTED,false));
        gap(authCard,6);
        authCard.addView(label("Endereço de conexão",12,MUTED,true));
        hostInput=input("Endereço de conexão",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        hostInput.setText(DEFAULT_SERVER);authCard.addView(hostInput);gap(authCard,7);
        authCard.addView(label("Código de acesso",12,MUTED,true));
        userInput=input("Seu código",InputType.TYPE_CLASS_TEXT);authCard.addView(userInput);gap(authCard,7);
        authCard.addView(label("Chave pessoal",12,MUTED,true));
        passInput=input("Sua chave",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        authCard.addView(passInput);gap(authCard,9);
        connect=button("ENTRAR NO MOREIRA TV   →",true);authCard.addView(connect);
        connect.setOnClickListener(v->login());
        authCard.addView(label("Acesso protegido apenas neste aparelho.",11,MUTED,false));
        status=label("Ative sua TV para começar.",12,BLUE,false);
        status.setPadding(dp(5),dp(8),dp(5),dp(7));base.addView(status);
        menu=vertical();menu.setVisibility(View.GONE);
        base.addView(menu,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        String[] captions={"◉ AO VIVO","▶ FILMES","▣ SÉRIES"},kinds={"live","movie","series"};
        for(int i=0;i<3;i++){
            tabs[i]=button(captions[i],i==0);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);
            if(i>0)p.leftMargin=dp(6);
            bar.addView(tabs[i],p);
            final int idx=i;
            tabs[i].setOnClickListener(v->{
                if(!loggedIn||type.equals(kinds[idx]))return;
                type=kinds[idx];updateTabs();loadCatalog();
            });
        }
        menu.addView(bar);gap(menu,12);
        menu.addView(label("ESCOLHA O QUE ASSISTIR",12,MUTED,true));
        categorySpinner=new Spinner(this);
        categories.add("Todas as categorias");categoryIds.add("all");updateCats();
        categorySpinner.setPadding(dp(12),dp(5),dp(12),dp(5));
        categorySpinner.setBackground(shape(SURFACE,BORDER,12));
        menu.addView(categorySpinner,new LinearLayout.LayoutParams(-1,dp(48)));
        gap(menu,10);
        searchInput=input("⌕  Pesquisar canais, filmes e séries",InputType.TYPE_CLASS_TEXT);
        menu.addView(searchInput);gap(menu,10);
        list=new ListView(this);list.setBackgroundColor(Color.TRANSPARENT);
        list.setCacheColorHint(Color.TRANSPARENT);list.setDivider(null);
        listAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,shownNames){
            @Override public View getView(int position,View previous,android.view.ViewGroup parent){
                LinearLayout card=new LinearLayout(MainActivity.this);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(dp(12),dp(8),dp(12),dp(8));
                card.setBackground(shape(SURFACE,BORDER,12));
                TextView icon=label("▶",17,ACCENT,true);
                icon.setGravity(Gravity.CENTER);icon.setBackground(shape(0xFF1D354D,0,10));
                card.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
                LinearLayout content=vertical();content.setPadding(dp(10),0,dp(6),0);
                TextView name=label(getItem(position),15,WHITE,true);name.setMaxLines(2);
                content.addView(name);content.addView(label("TOQUE PARA ASSISTIR",10,MUTED,false));
                card.addView(content,new LinearLayout.LayoutParams(0,-2,1));
                card.addView(label("›",25,ACCENT,true));
                LinearLayout wrap=vertical();wrap.setPadding(0,0,0,dp(6));wrap.addView(card);
                return wrap;
            }
        };
        list.setAdapter(listAdapter);
        menu.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        searchInput.setEnabled(false);categorySpinner.setEnabled(false);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filter();}
        });
        searchInput.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){filter();}
            public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((parent,v,pos,id)->{
            if(pos<shownItems.size()){
                JSONObject it=shownItems.get(pos);
                if("series".equals(type))showSeries(it);else play(it,type);
            }
        });
        updateTabs();
    }
    private void updateCats(){changing=true;categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories));changing=false;}
    private void setStatus(String s,boolean error){status.setText(s);status.setTextColor(error?Color.rgb(182,40,40):BLUE);}
    private String safeError(Exception e){return e instanceof java.net.UnknownHostException?"Servidor não encontrado (DNS). Verifique o endereço e a internet.": e instanceof java.net.SocketTimeoutException?"Servidor não respondeu dentro do tempo limite.":e instanceof java.net.ConnectException?"Conexão recusada pelo servidor.":"Não foi possível conectar: "+e.getClass().getSimpleName();}
    private String normalize(String v){String s=v.trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);if(!s.startsWith("http://")&&!s.startsWith("https://"))s="http://"+s;return s;}
    private void login(){
        String h=hostInput.getText().toString().trim(),u=userInput.getText().toString().trim(),p=passInput.getText().toString();
        if(h.isEmpty()||u.isEmpty()||p.isEmpty()){setStatus("Preencha servidor, usuário e senha.",true);return;}
        server=normalize(h);username=u;password=p;loggedIn=false;
        connect.setEnabled(false);setStatus("Testando autenticação no servidor…",false);
        worker.execute(()->{
            try{
                JSONObject info=fetchObject(null,null);
                JSONObject usr=info.optJSONObject("user_info");
                if(usr==null||!"1".equals(String.valueOf(usr.opt("auth"))))throw new IllegalStateException("Acesso não autorizado pelo servidor");
                String st=usr.optString("status","");
                if(st.toLowerCase(Locale.ROOT).contains("expir")||st.equalsIgnoreCase("disabled")||st.equalsIgnoreCase("banned"))throw new IllegalStateException("Assinatura sem acesso ativo");
                runOnUiThread(()->{loggedIn=true;connect.setEnabled(true);kindSpinner.setEnabled(true);categorySpinner.setEnabled(true);searchInput.setEnabled(true);setStatus("Conectado! Carregando TV ao vivo…",false);type="live";kindSpinner.setSelection(0);loadCatalog();});
            }catch(Exception ex){runOnUiThread(()->{connect.setEnabled(true);setStatus(ex instanceof IllegalStateException?ex.getMessage():safeError(ex),true);});}
        });
    }
    private String encoded(String v)throws Exception{return URLEncoder.encode(v,"UTF-8");}
    private String endpoint(String action, String extra)throws Exception{
        StringBuilder out=new StringBuilder(server+"/player_api.php?username="+encoded(username)+"&password="+encoded(password));
        if(action!=null)out.append("&action=").append(encoded(action));if(extra!=null)out.append(extra);return out.toString();
    }
    private String read(URL url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(25000);c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent","MoreiraTV-Android/1.0");
        try{
            int code=c.getResponseCode();if(code<200||code>=300)throw new java.io.IOException("HTTP "+code);
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>24*1024*1024)throw new java.io.IOException("Catálogo muito grande");out.write(b,0,n);}
            }return out.toString("UTF-8");
        }finally{c.disconnect();}
    }
    private JSONObject fetchObject(String action,String extra)throws Exception{return new JSONObject(read(new URL(endpoint(action,extra))));}
    private JSONArray fetchArray(String action,String extra)throws Exception{return new JSONArray(read(new URL(endpoint(action,extra))));}
    private void loadCatalog(){
        if(!loggedIn)return;
        final String requestedType=type;final int current=++generation;setStatus("Carregando catálogo…",false);searchInput.setText("");
        allItems.clear();shownItems.clear();shownNames.clear();listAdapter.notifyDataSetChanged();
        final String cAction=requestedType.equals("live")?"get_live_categories":requestedType.equals("movie")?"get_vod_categories":"get_series_categories";
        final String iAction=requestedType.equals("live")?"get_live_streams":requestedType.equals("movie")?"get_vod_streams":"get_series";
        worker.execute(()->{
            try{
                JSONArray cc=fetchArray(cAction,null),ii=fetchArray(iAction,null);
                List<String> cats=new ArrayList<>(),ids=new ArrayList<>();cats.add("Todas as categorias");ids.add("all");
                for(int i=0;i<cc.length();i++){JSONObject item=cc.optJSONObject(i);if(item==null)continue;cats.add(item.optString("category_name","Sem nome"));ids.add(item.optString("category_id",""));}
                List<JSONObject> parsed=new ArrayList<>();for(int i=0;i<ii.length();i++){JSONObject item=ii.optJSONObject(i);if(item!=null)parsed.add(item);}
                runOnUiThread(()->{if(current!=generation)return;categories.clear();categories.addAll(cats);categoryIds.clear();categoryIds.addAll(ids);allItems.addAll(parsed);updateCats();filter();setStatus("Conectado • "+parsed.size()+" itens carregados",false);});
            }catch(Exception e){runOnUiThread(()->{if(current==generation)setStatus("Falha ao carregar catálogo: "+safeError(e),true);});}
        });
    }
    private String titleOf(JSONObject o){return o.optString("name",o.optString("title",o.optString("series_name","Sem título")));}
    private void filter(){if(listAdapter==null)return;String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);int pos=categorySpinner.getSelectedItemPosition();String cat=pos>=0&&pos<categoryIds.size()?categoryIds.get(pos):"all";shownItems.clear();shownNames.clear();
        for(JSONObject x:allItems){if(!cat.equals("all")&&!cat.equals(x.optString("category_id","")))continue;String label=titleOf(x);if(!label.toLowerCase(Locale.ROOT).contains(q))continue;shownItems.add(x);shownNames.add(label);if(shownItems.size()>=3000)break;}
        listAdapter.notifyDataSetChanged();
    }
    private void play(JSONObject obj,String kind){
        String id=kind.equals("series")?obj.optString("episode_id",obj.optString("id")):obj.optString("stream_id","");if(id.isEmpty()){setStatus("Item sem ID de stream.",true);return;}
        String ext=kind.equals("live")?"m3u8":obj.optString("container_extension","mp4");if(ext.isEmpty())ext="mp4";
        Intent i=new Intent(this,PlayerActivity.class);i.putExtra("server",server);i.putExtra("username",username);i.putExtra("password",password);i.putExtra("type",kind);i.putExtra("id",id);i.putExtra("ext",ext);i.putExtra("title",titleOf(obj));startActivity(i);
    }
    private void showSeries(JSONObject s){
        String sid=s.optString("series_id","");if(sid.isEmpty()){setStatus("Série sem ID.",true);return;}
        setStatus("Carregando episódios…",false);
        worker.execute(()->{
            try{
                JSONObject details=fetchObject("get_series_info","&series_id="+encoded(sid));JSONObject eps=details.optJSONObject("episodes");if(eps==null)throw new IllegalStateException("Sem episódios");
                List<JSONObject> items=new ArrayList<>();List<String> names=new ArrayList<>();java.util.Iterator<String> it=eps.keys();while(it.hasNext()){String season=it.next();JSONArray a=eps.optJSONArray(season);if(a==null)continue;for(int k=0;k<a.length();k++){JSONObject ep=a.optJSONObject(k);if(ep==null)continue;ep.put("episode_id",ep.optString("id"));ep.put("name","T"+season+" • Ep "+ep.optString("episode_num",String.valueOf(k+1))+" — "+ep.optString("title","Episódio"));items.add(ep);names.add(ep.optString("name"));}}
                runOnUiThread(()->{setStatus("Escolha um episódio",false);if(names.isEmpty()){setStatus("Nenhum episódio disponível.",true);return;}new AlertDialog.Builder(this).setTitle(titleOf(s)).setItems(names.toArray(new String[0]),(d,index)->play(items.get(index),"series")).setNegativeButton("Fechar",null).show();});
            }catch(Exception e){runOnUiThread(()->setStatus("Não foi possível obter episódios: "+safeError(e),true));}
        });
    }
    @Override protected void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
}
