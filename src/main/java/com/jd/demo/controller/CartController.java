package com.jd.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CartController {

    // 模拟内存中的购物车
    private final List<String> cart = new ArrayList<>();

    // 固定 Token
    private static final String FIXED_TOKEN = "jd_test_token_12345";

    /**
     * POST /api/login - 模拟登录，返回固定 Token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        response.put("token", FIXED_TOKEN);
        response.put("message", "登录成功");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cart/clear - 清空购物车
     */
    @PostMapping("/cart/clear")
    public ResponseEntity<Map<String, Object>> clearCart(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        // 验证 Token
        if (authHeader == null || !authHeader.equals("Bearer " + FIXED_TOKEN)) {
            response.put("success", false);
            response.put("message", "Token 无效");
            return ResponseEntity.status(401).body(response);
        }

        cart.clear();
        response.put("success", true);
        response.put("message", "购物车已清空");
        response.put("cartCount", cart.size());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cart/count - 获取购物车商品数量
     */
    @GetMapping("/cart/count")
    public ResponseEntity<Map<String, Object>> getCartCount(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        // 验证 Token
        if (authHeader == null || !authHeader.equals("Bearer " + FIXED_TOKEN)) {
            response.put("success", false);
            response.put("message", "Token 无效");
            return ResponseEntity.status(401).body(response);
        }

        response.put("success", true);
        response.put("count", cart.size());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cart/add - 添加商品到购物车（内部使用）
     */
    @PostMapping("/cart/add")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                         @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        // 验证 Token
        if (authHeader == null || !authHeader.equals("Bearer " + FIXED_TOKEN)) {
            response.put("success", false);
            response.put("message", "Token 无效");
            return ResponseEntity.status(401).body(response);
        }

        String productId = request.get("productId");
        cart.add(productId);
        response.put("success", true);
        response.put("message", "商品已加入购物车");
        response.put("cartCount", cart.size());
        return ResponseEntity.ok(response);
    }
}
