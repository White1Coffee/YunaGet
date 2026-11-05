package nl.yunaget.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import nl.yunaget.client.core.*;
import java.util.*;
import java.util.function.*;

public final class YunaGetScreen extends Screen {
    private enum Tab { Dashboard, Items, Mining, Collector, ESP, Storage, Settings }
    private Tab tab = Tab.Dashboard;
    private int left, top, contentX, panelWidth, rows, page, totalPages = 1;
    private String query = "", feedback = "";
    private Identifier selected = Keybinds.item;
    private boolean entityView, storageOptions, oreEditor, targetsOnly;
    private StorageRule editingChest;
    private Keybinds.Action capturing;
    private final List<EditBox> textFields = new ArrayList<>();
    private EditBox search;
    private final List<String> labels = new ArrayList<>();
    private final List<Runnable> rowBuilders = new ArrayList<>();
    private final Map<Button,Identifier> inventoryButtons = new HashMap<>();
    private record Icon(net.minecraft.world.item.ItemStack stack,int y){}
    private final List<Icon> icons=new ArrayList<>();
    private String draftName="", draftFilters="", draftCategory="Wood", draftCategoryFilters="#minecraft:logs,minecraft:oak_planks", draftOreColor="55FFFF", draftPreset="Mijn preset";
    private int buildRow;

    public YunaGetScreen() { super(Component.literal("YunaGet v0.2.1")); }
    private YunaGetConfig c() { return YunaGetConfig.get; }
    @Override protected void init() {
        clearWidgets(); textFields.clear(); labels.clear(); rowBuilders.clear(); inventoryButtons.clear(); icons.clear(); search = null;
        panelWidth = Math.min(780, width - 20); left = (width - panelWidth)/2; top = 10; contentX = left + 112;
        rows = Math.max(1, (height - 116)/24);
        int index = 0;
        for (Tab t : Tab.values()) {
            button((tab == t ? "> " : "") + t.name(),left+8,top + 34 + index++*Math.max(16,Math.min(24,(height-114)/7)),94,19,() -> {
                tab = t; page = 0; editingChest = null; storageOptions = false; capturing = null; feedback = ""; init();
            });
        }
        button("X",left+panelWidth-22,top,22,20,this::onClose);
        button("NOODSTOP",left+8,height-52,94,20,()->{TaskManager.stop();init();});
        switch (tab) {
            case Dashboard -> dashboard(); case Items -> itemRows(false);
            case Mining -> miningOptions(); case Collector -> collectorOptions();
            case ESP -> espOptions(); case Storage -> storage(); case Settings -> settings();
        }
        if(tab!=Tab.Items&&!(tab==Tab.ESP&&entityView)){
            totalPages = Math.max(1,(rowBuilders.size()+rows-1)/rows); page = Math.clamp(page,0,totalPages-1);
            buildRow = 0;
            for (int i = page*rows; i < Math.min(rowBuilders.size(),(page+1)*rows); i++) { rowBuilders.get(i).run(); buildRow++; }
        }
        int bottom = height-59;
        button("<",contentX,bottom,28,20,() -> { page--; capturing=null; init(); }).active = page > 0;
        button(">",left+panelWidth-30,bottom,28,20,() -> { page++; capturing=null; init(); }).active = page+1 < totalPages;
    }
    private int rowY() { return top+36+buildRow*24; }
    private int contentWidth() { return panelWidth-124; }
    private Button button(String text,int x,int y,int w,int h,Runnable click) {
        return addRenderableWidget(Button.builder(Component.literal(text),b -> click.run()).bounds(x,y,w,h).build());
    }
    private void action(String text,Runnable action) {
        rowBuilders.add(() -> button(text,contentX,rowY(),contentWidth(),20,action));
    }
    private void note(String text) { rowBuilders.add(() -> { labels.add((rowY()+6)+":"+text); }); }
    private void toggle(String label,BooleanSupplier get,Consumer<Boolean> set) {
        action(label+": "+(get.getAsBoolean()?"AAN":"UIT"),() -> { set.accept(!get.getAsBoolean()); YunaGetConfig.save(); init(); });
    }
    private EditBox field(String label,String initial,int x,int y,int w,Consumer<String> change) {
        EditBox box = new EditBox(font,x,y,w,20,Component.literal(label));
        box.setMaxLength(4096); box.setValue(initial); box.setResponder(change); textFields.add(box); addRenderableWidget(box); return box;
    }
    private void text(String label,String value,Consumer<String> change) {
        rowBuilders.add(() -> {
            int labelWidth = Math.min(148, contentWidth()/2);
            labels.add((rowY()+6)+":"+label);
            field(label,value,contentX+labelWidth,rowY(),contentWidth()-labelWidth,change);
        });
    }
    private void number(String label,int value,int min,int max,IntConsumer set) {
        text(label,""+value,input -> {
            try { int n = Integer.parseInt(input); if(n<min||n>max) throw new NumberFormatException(); set.accept(n); YunaGetConfig.save(); feedback=""; }
            catch(NumberFormatException ex) { feedback=label+": kies "+min+"-"+max; }
        });
    }
    private void dashboard() {
        action("Start automatisch verzamelen en minen",this::startAutomatic);
        action(MiningTask.active()?"Mine-aura stoppen":"Alleen minen starten",() -> { if(MiningTask.active()) MiningTask.stop(""); else MiningTask.start(); minecraft.gui.setScreen(null); });
        action(TaskManager.paused?"Hervatten":"Pauzeren",() -> { Keybinds.run(Keybinds.Action.PAUSE); init(); });
        action("STOP alle acties",() -> { TaskManager.stop(); init(); });
        action("Nu inventory sorteren",AutoSorterManager::start);
        note("AutoTool "+(c().autoTool?"AAN":"UIT")+" | Ore ESP "+(c().oreEsp?"AAN":"UIT")+" | Chest-glow "+(c().chestGlow?"AAN":"UIT"));
        action("Items aanvinken ("+c().selectedItems.size()+" geselecteerd)",()->navigate(Keybinds.Action.ITEMS));
        note("Geen aantallen nodig. Dichtstbijzijnde bruikbare doel eerst.");
    }
    private void startAutomatic(){
        if(c().selectedItems.isEmpty()){feedback="Vink eerst een of meer items aan onder Items.";return;}
        c().collector=true;c().selectedDropsOnly=true;MiningTask.start();YunaGetConfig.save();minecraft.gui.setScreen(null);
    }
    private void itemRows(boolean entities) {
        int w=contentWidth();
        search=field("Zoek op naam of ID",query,contentX,top+29,w-112,input->{
            int caret=search==null?input.length():search.getCursorPosition();
            query=input;page=0;init();setFocused(search);search.setCursorPosition(Math.min(caret,input.length()));
        });
        search.setMaxLength(100);
        button(entities?"Terug naar ESP":targetsOnly?"Alle items":"Mijn selectie",contentX+w-106,top+29,106,20,()->{
            if(entities){entityView=false;page=0;init();}else{targetsOnly=!targetsOnly;page=0;init();}
        });
        if(!entities){
            String[] groupNames={"Ertsen","Hout","Bouwblokken"};int groupWidth=(w-8)/3;
            for(int i=0;i<groupNames.length;i++){
                String group=groupNames[i];
                button(group,contentX+i*(groupWidth+4),top+53,groupWidth,20,()->{
                    var values=ItemGroups.defaults().get(group).stream().filter(id->BuiltInRegistries.ITEM.containsKey(Identifier.parse(id))).toList();
                    if(!ItemGroups.add(c().selectedItems,values,128))feedback="Maximaal 128 aangevinkte items.";
                    YunaGetConfig.save();init();
                });
            }
            button("Start automatisch",contentX,top+77,(w-4)/2,20,this::startAutomatic);
            button("Alles uitvinken",contentX+(w+4)/2,top+77,(w-4)/2,20,()->{
                c().selectedItems.clear();MiningTask.stop("");DropCollectorManager.reset();NavigationManager.cancel();YunaGetConfig.save();init();
            });
        }else{
            button("Entity Glow: "+(c().entityGlow?"AAN":"UIT"),contentX,top+53,w,20,()->{c().entityGlow=!c().entityGlow;YunaGetConfig.save();init();});
        }
        int listTop=top+(entities?79:103);
        int visible=Math.max(1,(height-66-listTop)/24);
        var ids=new ArrayList<Identifier>();
        if(entities)ids.addAll(BuiltInRegistries.ENTITY_TYPE.keySet());else ids.addAll(BuiltInRegistries.ITEM.keySet());
        if(!entities)ids.removeIf(id->id.toString().equals("minecraft:air"));
        if(!entities&&targetsOnly)ids.removeIf(id->!c().selectedItems.contains(id.toString()));
        String term=query.toLowerCase(Locale.ROOT).trim();
        ids.removeIf(id->!id.toString().contains(term)&&!displayName(id,entities).toLowerCase(Locale.ROOT).contains(term));
        ids.sort(Comparator.comparing(id->displayName(id,entities)));
        totalPages=Math.max(1,(ids.size()+visible-1)/visible);page=Math.clamp(page,0,totalPages-1);
        int index=0;
        for(int i=page*visible;i<Math.min(ids.size(),(page+1)*visible);i++){
            Identifier id=ids.get(i);int y=listTop+index++*24;
            boolean checked=entities?id.equals(Keybinds.entity):c().selectedItems.contains(id.toString());
            Runnable choose=()->{
                selected=id;
                if(entities)Keybinds.entity=id;else{
                    Keybinds.item=id;
                    if(!ItemSelection.toggle(c().selectedItems,id.toString(),128))feedback="Maximaal 128 aangevinkte items.";
                    MiningTask.suspend();NavigationManager.cancel();
                }
                Keybinds.save();YunaGetConfig.save();init();
            };
            button(checked?"[x]":"[ ]",contentX,y,24,20,choose);
            Button row=button(displayName(id,entities)+(entities?"":"  |  "+InventoryTracker.count(id.toString())+" in tas"),contentX+50,y,w-50,20,choose);
            row.setTooltip(Tooltip.create(Component.literal(id.toString())));
            if(!entities){
                inventoryButtons.put(row,id);icons.add(new Icon(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(id)),y+2));
            }
        }
        if(ids.isEmpty())labels.add((listTop+8)+":Geen items gevonden. Pas de zoektekst aan.");
    }
    private String displayName(Identifier id,boolean entities){
        if(entities)return BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription().getString();
        return new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(id)).getHoverName().getString();
    }
    private void miningOptions() {
        action(MiningTask.active()?"Mining: AAN (klik om te stoppen)":"Mining: UIT (klik om te starten)",() -> { Keybinds.run(Keybinds.Action.AURA); init(); });
        number("Zoekradius",c().searchRadius,1,32,n->c().searchRadius=n);
        number("Interval (ticks)",c().miningInterval,1,20,n->c().miningInterval=n);
        toggle("Alleen zichtbare zoekdoelen",()->c().onlyVisible,v->c().onlyVisible=v);
        toggle("Xray van aangevinkte items",()->c().targetXray,v->c().targetXray=v);
        toggle("AutoTool",()->c().autoTool,v->c().autoTool=v);
        number("Min. durability",c().minDurability,0,1000,n->c().minDurability=n);
        toggle("Oorspronkelijk slot herstellen",()->c().restoreSlot,v->c().restoreSlot=v);
        note("Automatisch naar bekende blocks; minen vereist zicht en bereik.");
    }
    private void collectorOptions() {
        toggle("Drop Collector",()->c().collector,v->{c().collector=v;TaskManager.paused=false;if(!v)NavigationManager.cancel();});
        number("Verzamelradius",c().collectorRadius,1,32,n->c().collectorRadius=n);
        toggle("Alleen geselecteerde items",()->c().selectedDropsOnly,v->c().selectedDropsOnly=v);
        toggle("Veilige navigatie",()->c().safeNavigation,v->c().safeNavigation=v);
        action("Item-glow: "+(Keybinds.itemGlow?"AAN":"UIT"),()->{Keybinds.run(Keybinds.Action.GLOW);init();});
        action("Glow-filter: "+(Keybinds.glowSelectedOnly?"geselecteerde doelen":"alle drops"),()->{Keybinds.run(Keybinds.Action.GLOW_FILTER);init();});
        note("Handmatig lopen/jump/shift onderbreekt AutoWalk.");
        note("Geen teleportatie; alleen gewone pickup bij de speler.");
    }
    private void espOptions() {
        if(entityView) {
            itemRows(true); return;
        }
        action("Entitylijst en Entity Glow",()->{entityView=true;page=0;init();});
        toggle("Xray van aangevinkte items",()->c().targetXray,v->c().targetXray=v);
        toggle("Ore ESP",()->c().oreEsp,v->c().oreEsp=v);
        toggle("Gemarkeerde chest-glow",()->c().chestGlow,v->c().chestGlow=v);
        toggle("Chest-glow door muren",()->c().chestGlowThroughWalls,v->c().chestGlowThroughWalls=v);
        number("Scanradius",c().scanRadius,1,48,n->c().scanRadius=n);
        number("Renderafstand",c().renderDistance,1,96,n->c().renderDistance=n);
        toggle("Outlines door muren",()->c().throughWalls,v->c().throughWalls=v);
        action("Blocktypes / kleuren bewerken",()->{oreEditor=!oreEditor;page=0;init();});
        if(oreEditor) {
            text("Block-ID",query,v->query=v);
            text("Kleur (RRGGBB)",draftOreColor,v->draftOreColor=v);
            action("Blockkleur opslaan",()->{
                var id=Identifier.tryParse(query);
                try {
                    if(id==null||!BuiltInRegistries.BLOCK.containsKey(id)||!draftOreColor.matches("[0-9a-fA-F]{6}")||c().oreColors.size()>=128&&!c().oreColors.containsKey(id.toString()))throw new IllegalArgumentException();
                    c().oreColors.put(id.toString(),0xFF000000|(int)Long.parseLong(draftOreColor,16)); YunaGetConfig.save(); init();
                }catch(IllegalArgumentException ex){feedback="Geldig block-ID en 6 hextekens nodig (max.128)";}
            });
            action("Ingevoerd block verwijderen",()->{c().oreColors.remove(query);YunaGetConfig.save();init();});
            for(var e:c().oreColors.entrySet()) note(e.getKey()+" #"+String.format("%06X",e.getValue()&0xFFFFFF));
        }
        note("Alleen ontvangen wereldgegevens; max. 512 outlines/frame.");
    }
    private void storage() {
        if(editingChest!=null){editChest();return;}
        action("[+] Mark Chest (daarna kijken + marktoets/rechtsklik)",()->{
            ChestManager.marking=true; TaskManager.status="Kijk naar een chest en druk de marktoets of rechtsklik";minecraft.gui.setScreen(null);
        });
        action("Nu sorteren",AutoSorterManager::start);
        toggle("Gemarkeerde chest-glow",()->c().chestGlow,v->c().chestGlow=v);
        action(storageOptions?"Chestlijst tonen":"Sorteerinstellingen / categorieen",()->{storageOptions=!storageOptions;page=0;init();});
        if(storageOptions) {
            toggle("Auto Sorter",()->c().sorter,v->c().sorter=v);
            toggle("Sorteren bij bijna vol",()->c().sortWhenFull,v->c().sortWhenFull=v);
            number("Drempel (%)",c().inventoryThreshold,1,100,n->c().inventoryThreshold=n);
            toggle("AutoWalk naar chest",()->c().walkToChest,v->c().walkToChest=v);
            number("Max. chestafstand",c().chestDistance,1,48,n->c().chestDistance=n);
            number("Min. aantal behouden",c().keepAmount,0,100000,n->c().keepAmount=n);
            toggle("Hotbar overslaan",()->c().skipHotbar,v->c().skipHotbar=v);
            toggle("Fallback-chest toestaan",()->c().useFallback,v->c().useFallback=v);
            text("Wereldprofiel",c().worldProfile,v->{ if(v.matches("[A-Za-z0-9_-]{1,64}")){c().worldProfile=v;YunaGetConfig.save();} });
            text("Favorieten (komma)",String.join(",",c().favorites),v->{
                var values=tokens(v);if(values.stream().allMatch(s->Identifier.tryParse(s)!=null)){c().favorites=new LinkedHashSet<>(values);YunaGetConfig.save();}
            });
            text("Categorienaam",draftCategory,v->draftCategory=v); text("Categoriefilters",draftCategoryFilters,v->draftCategoryFilters=v);
            action("Categorie opslaan",()->{
                if(!draftCategory.matches("[A-Za-z0-9_ -]{1,64}")||!validFilters(tokens(draftCategoryFilters))){feedback="Ongeldige categorie/filters";return;}
                c().categories.put(draftCategory,tokens(draftCategoryFilters));YunaGetConfig.save();init();
            });
            action("Ingevoerde categorie verwijderen",()->{c().categories.remove(draftCategory);YunaGetConfig.save();init();});
            for(var entry:c().categories.entrySet())note("@"+entry.getKey()+" = "+String.join(",",entry.getValue()));
            note("Multiplayerwerelden op dezelfde server: kies eigen profiel.");
        }else{
            note("Profiel: "+c().worldProfile+" | "+ChestManager.dimension());
            for(var r:ChestManager.current()) {
                action(r.name+" - "+r.x+", "+r.y+", "+r.z,()->{editingChest=r;draftName=r.name;draftFilters=String.join(",",r.filters);page=0;init();});
                note(ChestManager.availability(r));
            }
        }
    }
    private static List<String> tokens(String raw){return Arrays.stream(raw.split(",")).map(String::trim).filter(s->!s.isEmpty()).distinct().toList();}
    private static boolean validFilters(List<String> values){return values.size()<=512&&values.stream().allMatch(ChestFilterManager::validToken);}
    private void editChest(){
        var r=editingChest;
        note("Chest "+r.x+", "+r.y+", "+r.z+" | "+ChestManager.availability(r));
        text("Naam",draftName,v->draftName=v);text("Filters (komma)",draftFilters,v->draftFilters=v);
        action("Naam en filters opslaan",()->{
            if(draftName.isBlank()||draftName.length()>64||!validFilters(tokens(draftFilters))){feedback="Naam max.64; filters: item-ID, #tag of @categorie";return;}
            r.name=draftName;r.filters=new ArrayList<>(tokens(draftFilters));YunaGetConfig.save();init();
        });
        number("Prioriteit (hoog eerst)",r.priority,-1000,1000,n->r.priority=n);
        toggle("Chest actief",()->r.enabled,v->r.enabled=v);
        toggle("Fallback voor overige items",()->r.fallback,v->r.fallback=v);
        action("Unmark (verwijder markering)",()->{c().chests.remove(r);YunaGetConfig.save();editingChest=null;page=0;init();});
        action("Terug naar chests",()->{editingChest=null;page=0;init();});
        note("Presets vervangen filters, prioriteit en fallbackinstelling.");
        for(var preset:ChestPreset.defaults().entrySet())action("Preset: "+preset.getKey(),()->{
            preset.getValue().apply(r);r.name=preset.getKey();draftName=r.name;draftFilters=String.join(",",r.filters);YunaGetConfig.save();init();
        });
        text("Eigen presetnaam",draftPreset,v->draftPreset=v);
        action("Bewaar huidige filters als preset",()->{
            if(draftPreset.isBlank()||draftPreset.length()>64||c().chestPresets.size()>=128&&!c().chestPresets.containsKey(draftPreset)){feedback="Kies een presetnaam (max.64 tekens, 128 presets)";return;}
            if(!validFilters(tokens(draftFilters))){feedback="Ongeldige filters";return;}
            c().chestPresets.put(draftPreset,new ChestPreset(tokens(draftFilters),r.priority,r.fallback));YunaGetConfig.save();init();
        });
        for(var preset:c().chestPresets.entrySet()) {
            action("Eigen preset: "+preset.getKey(),()->{preset.getValue().apply(r);draftFilters=String.join(",",r.filters);YunaGetConfig.save();init();});
            action("Preset verwijderen: "+preset.getKey(),()->{c().chestPresets.remove(preset.getKey());YunaGetConfig.save();init();});
        }
    }
    private void settings(){
        toggle("Compact statuspaneel",()->c().statusHud,v->c().statusHud=v);
        toggle("Lijn en route naar loopdoel",()->c().navigationLine,v->c().navigationLine=v);
        action("Configuratie opslaan",()->{YunaGetConfig.save();Keybinds.save();feedback=YunaGetConfig.error.isEmpty()?"Opgeslagen":YunaGetConfig.error;});
        action("Configuratie opnieuw laden",()->{
            TaskManager.pause(); MiningTask.stop(""); Keybinds.load(); YunaGetConfig.load();
            feedback="Config geladen; acties gepauzeerd";init();
        });
        note("Klik actie, kies toets; Backspace wist, Escape annuleert.");
        for(var action:Keybinds.Action.values()) {
            action(action.label+": "+(capturing==action?"Druk toets...":Keybinds.name(action)),()->{capturing=action;init();});
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xB0091019);
        g.fill(left-2,top-2,left+panelWidth+2,height-8,0xFF101822);
        g.fill(left,top,left+panelWidth,height-10,0xF01B2736);
        g.fill(left,top,left+106,height-10,0xFF121C29);
        g.fill(left,top,left+panelWidth,top+2,0xFF53E7D0);
        g.fill(contentX-6,top+26,left+panelWidth-8,top+27,0xFF34465B);
        g.fill(contentX-6,height-65,left+panelWidth-8,height-64,0xFF34465B);
        super.extractRenderState(g,mx,my,dt);
        g.text(font,"YUNAGET",left+12,top+12,0xFF53E7D0,true);
        g.text(font,tab+"  |  "+c().selectedItems.size()+" items aangevinkt",contentX,top+11,0xFFFFFFFF,true);
        g.outline(left+7,height-53,96,22,0xFFFF6677);
        for(String label:labels){int colon=label.indexOf(':');g.text(font,font.plainSubstrByWidth(label.substring(colon+1),contentWidth()),contentX,Integer.parseInt(label.substring(0,colon)),0xFFDDDDDD,false);}
        for(var icon:icons)g.item(icon.stack(),contentX+30,icon.y());
        g.text(font,"Pagina "+(page+1)+"/"+totalPages,contentX+40,height-53,0xFFFFFFFF,false);
        String status=!YunaGetConfig.error.isEmpty()?YunaGetConfig.error:!Keybinds.error.isEmpty()?Keybinds.error:
            feedback.isEmpty()?TaskManager.owner+" "+TaskManager.state+": "+TaskManager.status:feedback;
        g.text(font,font.plainSubstrByWidth(status,contentWidth()),contentX,height-32,0xFFFFCC77,false);
        g.text(font,font.plainSubstrByWidth("Aura "+(MiningTask.active()?"AAN":"UIT")+" | Collector "+(c().collector?"AAN":"UIT")+" | Tas "+InventoryTracker.used+"/36 | Noodstop "+Keybinds.name(Keybinds.Action.PANIC),contentWidth()),contentX,height-19,0xFF9EBDCF,false);
    }
    public void navigate(Keybinds.Action action){
        switch(action){
            case ITEMS->tab=Tab.Items;case ENTITIES->{tab=Tab.ESP;entityView=true;selected=Keybinds.entity;}
            case BINDS->tab=Tab.Settings;case DASHBOARD->tab=Tab.Dashboard;case MINING->tab=Tab.Mining;
            case COLLECTOR->tab=Tab.Collector;case ESP->{tab=Tab.ESP;entityView=false;}case STORAGE->tab=Tab.Storage;
            case NEXT->{page++;init();return;}case PREVIOUS->{page--;init();return;}
            case SEARCH,AMOUNT->{
                if(action==Keybinds.Action.AMOUNT)tab=Tab.Items;
                if(tab==Tab.Items||tab==Tab.ESP&&entityView){page=0;init();}
                if(search!=null)setFocused(search);else if(!textFields.isEmpty())setFocused(textFields.getFirst());return;
            }
            default->{return;}
        }
        page=0;capturing=null;init();
    }
    @Override public boolean keyPressed(KeyEvent event){
        int key=event.key();
        if(capturing!=null){
            if(key==InputConstants.KEY_ESCAPE)feedback="Geannuleerd";
            else feedback=Keybinds.bind(capturing,key==InputConstants.KEY_BACKSPACE||key==InputConstants.KEY_DELETE?-1:key);
            capturing=null;init();return true;
        }
        if(Keybinds.matching(key)==Keybinds.Action.STOP||Keybinds.matching(key)==Keybinds.Action.PANIC){Keybinds.consume(Keybinds.matching(key));TaskManager.stop();init();return true;}
        if(textFields.stream().anyMatch(EditBox::isFocused))return super.keyPressed(event);
        var action=Keybinds.matching(key);if(action==null)return super.keyPressed(event);
        if(!Keybinds.consume(action))return true;
        switch(action){
            case ITEMS,ENTITIES,BINDS,DASHBOARD,MINING,COLLECTOR,ESP,STORAGE,NEXT,PREVIOUS,SEARCH,AMOUNT->navigate(action);
            default->{Keybinds.run(action);if(minecraft.gui.screen()==this)init();}
        }
        return true;
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){
        super.tick();
        for(var entry:inventoryButtons.entrySet()){
            var id=entry.getValue();
            entry.getKey().setMessage(Component.literal(displayName(id,false)+"  |  "+InventoryTracker.count(id.toString())+" in tas"));
        }
    }
}
