package com.bonial.restaurantfinder;

import com.bonial.restaurantfinder.config.CityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CityProperties.class)
public class RestaurantFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantFinderApplication.class, args);
    }
}
