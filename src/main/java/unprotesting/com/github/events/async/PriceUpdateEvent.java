package unprotesting.com.github.events.async;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import unprotesting.com.github.Main;
import unprotesting.com.github.config.Config;
import unprotesting.com.github.data.ephemeral.data.EnchantmentData;
import unprotesting.com.github.data.ephemeral.data.ItemData;
import unprotesting.com.github.data.persistent.Database;
import unprotesting.com.github.data.persistent.TimePeriod;
import unprotesting.com.github.logging.Logging;
import unprotesting.com.github.util.UtilFunctions;

public class PriceUpdateEvent extends Event{

    @Getter
    private final HandlerList Handlers = new HandlerList();

    public PriceUpdateEvent(boolean isAsync){
        super(isAsync);
        if (Main.isCorrectAPIKey()){
            try {
                calculateAndLoad();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void calculateAndLoad() throws InterruptedException{
        int playerCount = UtilFunctions.calculatePlayerCount();
        Logging.debug("Updating prices with player count: " + playerCount);
        if (playerCount >= Config.getUpdatePricesThreshold()){
            Main.updateTimePeriod();
            updateItems();
            Thread.sleep(500);
            updateEnchantments();
            Main.getCache().updatePercentageChanges();
        }
        else{
            Logging.debug("Player count was less than threshhold: " +  Config.getUpdatePricesThreshold());
        }
        Logging.debug("Next update: " + LocalDateTime.now().plusMinutes(Config.getTimePeriod())
         .format(DateTimeFormatter.ISO_LOCAL_TIME).toString());
    }

    private void updateItems(){
        ConcurrentHashMap<String, ItemData> ITEMS = Main.getCache().getITEMS();
        for (String item : ITEMS.keySet()){
            if (Main.getDfiles().getShops().getConfigurationSection("shops").getConfigurationSection(item).getBoolean("locked")){
                continue;
            }
            ItemData data = ITEMS.get(item);
            double price = data.getPrice();
            Double[] buysell = loadAverageBuySellValue(item, price, false);
            Double newprice;
            Double total = buysell[0]+buysell[1];
            if (buysell[0] > buysell[1]){
                newprice = price + price*Config.getBasicMaxVariableVolatility()*0.01*(buysell[0]/total) + price*0.01*Config.getBasicMinVariableVolatility();
            }
            else if(buysell[0] < buysell[1]){
                newprice = price - price*Config.getBasicMaxVariableVolatility()*0.01*(buysell[1]/total) - price*0.01*Config.getBasicMinVariableVolatility();
            }
            else{
                newprice = price;
            }
            data.setPrice(newprice);
            ITEMS.put(item, data);
        }
        Main.getCache().updatePrices(ITEMS);
    }

    private void updateEnchantments(){
        ConcurrentHashMap<String, EnchantmentData> ENCHANTMENTS = Main.getCache().getENCHANTMENTS();
        for (String item : ENCHANTMENTS.keySet()){
            EnchantmentData data = ENCHANTMENTS.get(item);
            double price = data.getPrice();
            Double[] buysell = loadAverageBuySellValue(item, price, true);
            Double newprice;
            Double total = buysell[0]+buysell[1];
            if (buysell[0] > buysell[1]){
                newprice = price + price*Config.getBasicMaxVariableVolatility()*0.01*(buysell[0]/total) + price*0.01*Config.getBasicMinVariableVolatility();
            }
            else if(buysell[0] < buysell[1]){
                newprice = price - price*Config.getBasicMaxVariableVolatility()*0.01*(buysell[1]/total) - price*0.01*Config.getBasicMinVariableVolatility();
            }
            else{
                newprice = price;
            }
            data.setPrice(newprice);
            ENCHANTMENTS.put(item, data);
        }
        Main.getCache().updateEnchantments(ENCHANTMENTS);
    }

    private Double[] loadAverageBuySellValue(String item, Double price, boolean enchantment){
        double x = 0;
        int size = Main.getDatabase().map.size();
        Database db = Main.getDatabase();
        double final_y = 1;
        double final_buys = 0;
        double final_sells = 0;
        for (;x < 1000000;){
            double y = Config.getDataSelectionM() * Math.pow(x, Config.getDataSelectionZ()) + Config.getDataSelectionC();
            y = Math.round(y);
            if (y > size){
                final_y = y;
                break;
            }
            int input = (int) y;
            TimePeriod period = db.map.get(size-input);
            double buys = 0;
            double sells = 0;
            try{
                if (enchantment){
                    int loc = Arrays.asList(period.getEtp().getItems()).indexOf(item);
                    buys = period.getEtp().getBuys()[loc];
                    sells = period.getEtp().getSells()[loc];
                }
                if (!enchantment){
                    int loc = Arrays.asList(period.getItp().getItems()).indexOf(item);
                    buys = period.getItp().getBuys()[loc];
                    sells = period.getItp().getSells()[loc];
                }
            }
            catch(NullPointerException e){
                e.printStackTrace();
                break;
            }
            final_buys += buys;
            final_sells += sells;
            x++;
        }
        if (Config.isInflationEnabled()){
            final_buys += final_buys * 0.01 * Config.getInflationValue();
        }
        double avBuy = final_buys / final_y;
        double avSell = final_sells / final_y;
        return new Double[]{avBuy, avSell};
    }
    
}