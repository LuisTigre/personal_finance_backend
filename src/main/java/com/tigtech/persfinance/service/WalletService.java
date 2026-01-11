package com.tigtech.persfinance.service;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.web.dto.*;

import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletResponse createWallet(User creator, CreateWalletRequest request);
    WalletResponse addMember(UUID walletId, User requester, AddWalletMemberRequest request);
    List<WalletResponse> getWalletsForUser(User user);
    WalletResponse getWalletDetails(UUID walletId, User user);
    WalletResponse archiveWallet(UUID walletId, User user);
    WalletResponse unarchiveWallet(UUID walletId, User user);
    void deleteWallet(UUID walletId, User user);
}
