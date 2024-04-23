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
        fetchData(message);
        latch.countDown();
    }

    private void fetchData(String stockSymbol) {
        String url = stockApiUrl + stockSymbol;
        StockSymbol stock = restTemplate.getForObject(url, StockSymbol.class);
        if (stock != null) {
            System.out.println("Fetched Stock: " + stock.getName() + " - Last Sale: " + stock.getLastSale());
        } else {
            System.out.println("Failed to fetch data for stock symbol: " + stockSymbol);
        }
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
