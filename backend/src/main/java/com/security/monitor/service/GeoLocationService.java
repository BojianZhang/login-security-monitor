package com.security.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * IP地理位置服务
 */
@Service
public class GeoLocationService {

    private static final Logger logger = LoggerFactory.getLogger(GeoLocationService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${security.monitor.geo.api-key:}")
    private String apiKey;

    public GeoLocationService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取IP地理位置信息（带缓存）
     */
    @Cacheable(value = "geoLocation", key = "#ipAddress")
    public GeoLocationInfo getGeoLocation(String ipAddress) {
        if (isPrivateIP(ipAddress)) {
            return createLocalGeoInfo();
        }

        try {
            // 首先尝试免费的ipapi.co服务
            GeoLocationInfo info = getLocationFromIpApi(ipAddress);
            if (info != null) {
                return info;
            }

            // 如果第一个API失败，尝试ip-api.com
            info = getLocationFromIpApiCom(ipAddress);
            if (info != null) {
                return info;
            }

            // 如果都失败，返回默认值
            logger.warn("无法获取IP地理位置信息: {}", ipAddress);
            return createUnknownGeoInfo();

        } catch (Exception e) {
            logger.error("获取IP地理位置信息时发生错误: {}", ipAddress, e);
            return createUnknownGeoInfo();
        }
    }

    /**
     * 使用ipapi.co获取地理位置信息
     */
    private GeoLocationInfo getLocationFromIpApi(String ipAddress) {
        try {
            String url = String.format("https://ipapi.co/%s/json/", ipAddress);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // 检查是否有错误
                if (jsonNode.has("error") && jsonNode.get("error").asBoolean()) {
                    return null;
                }

                return GeoLocationInfo.builder()
                        .ipAddress(ipAddress)
                        .country(getStringValue(jsonNode, "country_name"))
                        .region(getStringValue(jsonNode, "region"))
                        .city(getStringValue(jsonNode, "city"))
                        .latitude(getBigDecimalValue(jsonNode, "latitude"))
                        .longitude(getBigDecimalValue(jsonNode, "longitude"))
                        .timezone(getStringValue(jsonNode, "timezone"))
                        .isp(getStringValue(jsonNode, "org"))
                        .build();
            }
        } catch (Exception e) {
            logger.debug("ipapi.co获取失败: {}", ipAddress, e);
        }
        return null;
    }

    /**
     * 使用ip-api.com获取地理位置信息
     */
    private GeoLocationInfo getLocationFromIpApiCom(String ipAddress) {
        try {
            String url = String.format("http://ip-api.com/json/%s", ipAddress);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                
                // 检查请求是否成功
                if ("fail".equals(getStringValue(jsonNode, "status"))) {
                    return null;
                }

                return GeoLocationInfo.builder()
                        .ipAddress(ipAddress)
                        .country(getStringValue(jsonNode, "country"))
                        .region(getStringValue(jsonNode, "regionName"))
                        .city(getStringValue(jsonNode, "city"))
                        .latitude(getBigDecimalValue(jsonNode, "lat"))
                        .longitude(getBigDecimalValue(jsonNode, "lon"))
                        .timezone(getStringValue(jsonNode, "timezone"))
                        .isp(getStringValue(jsonNode, "isp"))
                        .build();
            }
        } catch (Exception e) {
            logger.debug("ip-api.com获取失败: {}", ipAddress, e);
        }
        return null;
    }

    /**
     * 检查是否为私有IP
     */
    private boolean isPrivateIP(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }
        
        return ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("10.") ||
               ipAddress.startsWith("172.") ||
               ipAddress.equals("127.0.0.1") ||
               ipAddress.equals("localhost") ||
               ipAddress.equals("0:0:0:0:0:0:0:1");
    }

    /**
     * 创建本地网络地理信息
     */
    private GeoLocationInfo createLocalGeoInfo() {
        return GeoLocationInfo.builder()
                .ipAddress("127.0.0.1")
                .country("本地网络")
                .region("本地网络")
                .city("本地网络")
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .timezone("Asia/Shanghai")
                .isp("本地网络")
                .build();
    }

    /**
     * 创建未知地理信息
     */
    private GeoLocationInfo createUnknownGeoInfo() {
        return GeoLocationInfo.builder()
                .ipAddress("Unknown")
                .country("Unknown")
                .region("Unknown")
                .city("Unknown")
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .timezone("Unknown")
                .isp("Unknown")
                .build();
    }

    /**
     * 从JSON节点获取字符串值
     */
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    /**
     * 从JSON节点获取BigDecimal值
     */
    private BigDecimal getBigDecimalValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 计算两点间距离（公里）
     */
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lonDistance = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371 * c; // 地球半径6371公里
    }

    /**
     * 地理位置信息对象
     */
    public static class GeoLocationInfo {
        private String ipAddress;
        private String country;
        private String region;
        private String city;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String timezone;
        private String isp;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final GeoLocationInfo info = new GeoLocationInfo();

            public Builder ipAddress(String ipAddress) {
                info.ipAddress = ipAddress;
                return this;
            }

            public Builder country(String country) {
                info.country = country;
                return this;
            }

            public Builder region(String region) {
                info.region = region;
                return this;
            }

            public Builder city(String city) {
                info.city = city;
                return this;
            }

            public Builder latitude(BigDecimal latitude) {
                info.latitude = latitude;
                return this;
            }

            public Builder longitude(BigDecimal longitude) {
                info.longitude = longitude;
                return this;
            }

            public Builder timezone(String timezone) {
                info.timezone = timezone;
                return this;
            }

            public Builder isp(String isp) {
                info.isp = isp;
                return this;
            }

            public GeoLocationInfo build() {
                return info;
            }
        }

        // Getters
        public String getIpAddress() { return ipAddress; }
        public String getCountry() { return country; }
        public String getRegion() { return region; }
        public String getCity() { return city; }
        public BigDecimal getLatitude() { return latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public String getTimezone() { return timezone; }
        public String getIsp() { return isp; }
    }
}