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


    private void fetchData(String stockSymbol) {
        String url = stockApiUrl + stockSymbol;
        try {
            StockSymbol stock = restTemplate.getForObject(url, StockSymbol.class);
            if (stock != null) {
                System.out.println("Fetched Stock: " + stock.getName() + " - Last Sale: " + stock.getLastSale());
            } else {
                System.out.println("No data found for stock symbol: " + stockSymbol);
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch data for stock symbol: " + stockSymbol + "; Error: " + e.getMessage());
        }
    }

    // Extracts the stock symbol from the message
    private String extractStockSymbol(String message) {
        if (message != null && message.startsWith("UPDATE:")) {
            return message.substring(7); // 7 is the length of "UPDATE:"
        }
        return null;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
