package com.personalfinance.app.controller;

import com.personalfinance.app.model.Account;
import com.personalfinance.app.model.User;
import com.personalfinance.app.service.AccountService;
import com.personalfinance.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    private final AccountService accountService;
    private final UserService userService;
    
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        List<Account> accounts = accountService.getUserAccounts(user);
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }
    
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account, Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        account.setUser(user);
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        Account updatedAccount = accountService.updateAccount(id, account);
        return ResponseEntity.ok(updatedAccount);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
