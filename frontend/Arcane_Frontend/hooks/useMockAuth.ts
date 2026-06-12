"use client";

import { useCallback, useEffect, useState } from "react";
import {
  AUTH_CHANGE_EVENT,
  AUTH_STORAGE_KEY,
  getCurrentUser,
  startOAuthLogin,
  signOutMock,
} from "@/lib/mockAuth";
import type { AuthProvider, AuthUser } from "@/types/community";

export const useMockAuth = () => {
  const [user, setUser] = useState<AuthUser | null>(null);

  const refreshUser = useCallback(() => {
    setUser(getCurrentUser());
  }, []);

  useEffect(() => {
    refreshUser();

    const handleStorage = (event: StorageEvent) => {
      if (event.key === AUTH_STORAGE_KEY) {
        refreshUser();
      }
    };

    window.addEventListener(AUTH_CHANGE_EVENT, refreshUser);
    window.addEventListener("storage", handleStorage);

    return () => {
      window.removeEventListener(AUTH_CHANGE_EVENT, refreshUser);
      window.removeEventListener("storage", handleStorage);
    };
  }, [refreshUser]);

  const signIn = useCallback((provider: AuthProvider = "google") => {
    startOAuthLogin(provider);
  }, []);

  const signOut = useCallback(() => {
    signOutMock();
    setUser(null);
  }, []);

  return {
    user,
    isAuthenticated: Boolean(user),
    signIn,
    signOut,
  };
};
