package com.example.dataanalyzer.rabbitmq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.example.dataanalyzer.model.StockSymbol;
import java.util.concurrent.CountDownLatch;

@Component
public class Receiver {
    @Value("${app.stock.api.url}")
    private String stockApiUrl;
    private final RestTemplate restTemplate;
    private CountDownLatch latch = new CountDownLatch(1);

    // Constructor injection of RestTemplate
    public Receiver(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     *
     * @param message
     */
    public void receiveMessage(String message) {
        System.out.println("Received <" + message + ">");
        String stockSymbol = extractStockSymbol(message);
        if (stockSymbol != null) {
            fetchData(stockSymbol);
        } else {
            System.out.println("Message format error, no stock symbol found.");
        }
        latch.countDown();
    }

    /**
     *  Fetching the stock data
     * @param stockSymbol
     */
    private void fetchData(String stockSymbol) {
        String url = stockApiUrl + stockSymbol;
        try {
            StockSymbol stock = restTemplate.getForObject(url, StockSymbol.class);
            if (stock != null) {
                float volatilityIndex = calculateVolatilityIndex(stock.getLastSale(), stock.getHighSale(), stock.getLowSale());
                stock.setVolatilityIndex(volatilityIndex);
                System.out.println("Fetched Stock: " + stock.getName() + " - Last Sale: " + stock.getLastSale() + " - Volatility Index: " + stock.getVolatilityIndex());

                if (volatilityIndex > 0.00) {
                    updateStockData(stock);
                }
            } else {
                System.out.println("No data found for stock symbol: " + stockSymbol);
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch data for stock symbol: " + stockSymbol + "; Error: " + e.getMessage());
        }
    }


    /**
     *
     * @param stock
     */
    private void updateStockData(StockSymbol stock) {
        String url = stockApiUrl + stock.getSymbol(); // Assuming the URL needs the stock symbol to update the correct entry
        try {
            restTemplate.put(url, stock); // Sending a PUT request to update the stock data
            System.out.println("Updated Stock: " + stock.getSymbol() + " - Volatility Index set to " + stock.getVolatilityIndex());
        } catch (Exception e) {
            System.out.println("Failed to update data for stock symbol: " + stock.getSymbol() + "; Error: " + e.getMessage());
        }
    }

    /**
     * Extracts the stock symbol from the message
     * @param message
     * @return
     */
    private String extractStockSymbol(String message) {
        if (message != null && message.startsWith("UPDATE:")) {
            return message.substring(7); // 7 is the length of "UPDATE:"
        }
        return null;
    }

    /**
     * calculates the volatility of a stock
     * @param lastSale
     * @param highSale
     * @param lowSale
     * @return
     */
    private float calculateVolatilityIndex(float lastSale, float highSale, float lowSale) {
        if (highSale == lowSale) return 0; // Avoid division by zero
        float midpoint = (highSale + lowSale) / 2;
        float range = highSale - lowSale;
        float distanceFromMidpoint = Math.abs(midpoint - lastSale);
        return 100 * (1 - (distanceFromMidpoint / range)); // Closer to midpoint implies less volatility
    }


    public CountDownLatch getLatch() {
        return latch;
    }
}
