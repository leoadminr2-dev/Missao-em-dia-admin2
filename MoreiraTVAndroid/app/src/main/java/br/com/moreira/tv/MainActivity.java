package br.com.moreira.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
    private static final int NAVY=Color.rgb(8,19,39), BLUE=Color.rgb(27,87,165);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private EditText hostInput, userInput, passInput, searchInput;
    private TextView status, title;
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

    @Override public void onCreate(Bundle state){super.onCreate(state);buildUi();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(2),dp(5),dp(2),dp(5));if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private EditText input(String hint, int inputType){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(true);e.setInputType(inputType);e.setPadding(dp(12),dp(9),dp(12),dp(9));e.setBackgroundColor(Color.WHITE);return e;}
    private LinearLayout root(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(15),dp(13),dp(15),dp(9));v.setBackgroundColor(Color.rgb(235,241,247));return v;}
    private void buildUi(){
        LinearLayout base=root();setContentView(base);
        title=label("MOREIRA TV",27,NAVY,true);base.addView(title);
        base.addView(label("Player privado • Android",13,BLUE,true));
        LinearLayout auth=new LinearLayout(this);auth.setOrientation(LinearLayout.VERTICAL);auth.setPadding(0,dp(5),0,dp(10));base.addView(auth);
        auth.addView(label("Servidor",14,NAVY,true));hostInput=input("http://servidor.exemplo",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);hostInput.setText("http://aurinhatv.com");auth.addView(hostInput);
        auth.addView(label("Usuário",14,NAVY,true));userInput=input("Usuário fornecido pelo provedor",InputType.TYPE_CLASS_TEXT);auth.addView(userInput);
        auth.addView(label("Senha",14,NAVY,true));passInput=input("Senha fornecida pelo provedor",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);auth.addView(passInput);
        connect=new Button(this);connect.setText("CONECTAR AO SERVIDOR");auth.addView(connect);connect.setOnClickListener(v->login());
        status=label("Digite os dados de acesso. Nenhuma senha é gravada no celular.",13,BLUE,false);base.addView(status);
        kindSpinner=new Spinner(this);kindSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"TV AO VIVO","FILMES","SÉRIES"}));base.addView(kindSpinner);
        categorySpinner=new Spinner(this);categories.add("Todas as categorias");categoryIds.add("all");updateCats();base.addView(categorySpinner);
        searchInput=input("Pesquisar conteúdo",InputType.TYPE_CLASS_TEXT);base.addView(searchInput);
        list=new ListView(this);listAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,shownNames);list.setAdapter(listAdapter);list.setDividerHeight(dp(1));base.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        searchInput.setEnabled(false);kindSpinner.setEnabled(false);categorySpinner.setEnabled(false);
        kindSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!changing&&loggedIn){type=new String[]{"live","movie","series"}[pos];loadCatalog();}}});
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filter();}});
        searchInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){filter();}public void afterTextChanged(Editable e){}});
        list.setOnItemClickListener((parent,v,pos,id)->{if(pos<shownItems.size()){JSONObject it=shownItems.get(pos); if("series".equals(type))showSeries(it);else play(it,type);}});
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
