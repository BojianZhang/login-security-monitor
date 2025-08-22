import request from '@/utils/request'

// 邮件搜索API
export const emailSearchApi = {
  // 快速搜索
  quickSearch(query: string, page: number = 0, size: number = 20) {
    return request({
      url: '/email/search/quick',
      method: 'get',
      params: { q: query, page, size }
    })
  },

  // 全文搜索邮件
  searchMessages(searchRequest: {
    query?: string
    fromAddress?: string
    toAddress?: string
    subject?: string
    bodyText?: string
    folderId?: number
    dateFrom?: string
    dateTo?: string
    isRead?: boolean
    hasAttachments?: boolean
    priorityLevel?: number
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: string
    fullTextSearch?: boolean
    highlightEnabled?: boolean
  }) {
    return request({
      url: '/email/search/messages',
      method: 'post',
      data: searchRequest
    })
  },

  // 高级搜索
  advancedSearch(searchParams: {
    fromAddress?: string
    toAddress?: string
    subject?: string
    bodyText?: string
    folderId?: number
    dateFrom?: string
    dateTo?: string
    isRead?: boolean
    hasAttachments?: boolean
    priorityLevel?: number
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: string
  }) {
    return request({
      url: '/email/search/advanced',
      method: 'post',
      data: searchParams
    })
  },

  // 搜索附件
  searchAttachments(searchRequest: {
    filename?: string
    contentType?: string
    minSize?: number
    maxSize?: number
    dateFrom?: string
    dateTo?: string
    page?: number
    size?: number
  }) {
    return request({
      url: '/email/search/attachments',
      method: 'post',
      data: searchRequest
    })
  },

  // 获取搜索建议
  getSearchSuggestions(query: string, limit: number = 10) {
    return request({
      url: '/email/search/suggestions',
      method: 'get',
      params: { q: query, limit }
    })
  },

  // 获取搜索历史
  getSearchHistory(limit: number = 20) {
    return request({
      url: '/email/search/history',
      method: 'get',
      params: { limit }
    })
  },

  // 删除搜索历史记录
  deleteSearchHistory(historyId: number) {
    return request({
      url: `/email/search/history/${historyId}`,
      method: 'delete'
    })
  },

  // 清空搜索历史
  clearSearchHistory() {
    return request({
      url: '/email/search/history',
      method: 'delete'
    })
  },

  // 获取邮件统计信息
  getEmailStatistics() {
    return request({
      url: '/email/search/statistics',
      method: 'get'
    })
  },

  // 导出搜索结果
  exportSearchResults(searchRequest: any, format: 'csv' | 'excel' | 'json' = 'csv') {
    return request({
      url: '/email/search/export',
      method: 'post',
      data: searchRequest,
      params: { format },
      responseType: 'blob'
    })
  }
}

// 搜索相关类型定义
export interface SearchResult {
  messages: EmailMessage[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
  searchQuery: string
  searchTime: number
}

export interface EmailMessage {
  id: number
  messageId: string
  subject: string
  fromAddress: string
  toAddresses: string
  ccAddresses?: string
  bccAddresses?: string
  replyTo?: string
  bodyText?: string
  bodyHtml?: string
  priorityLevel: number
  messageSize: number
  isRead: boolean
  isStarred: boolean
  isDeleted: boolean
  sentAt?: string
  receivedAt: string
  folder: EmailFolder
  user: User
  attachments?: EmailAttachment[]
}

export interface EmailFolder {
  id: number
  folderName: string
  folderType: 'INBOX' | 'SENT' | 'DRAFTS' | 'TRASH' | 'SPAM' | 'CUSTOM'
  messageCount: number
  unreadCount: number
  isSystemFolder: boolean
  createdAt: string
}

export interface EmailAttachment {
  id: number
  filename: string
  contentType: string
  fileSize: number
  contentId?: string
  isInline: boolean
  storagePath: string
  fileHash: string
}

export interface User {
  id: number
  username: string
  email: string
  displayName?: string
}

export interface SearchSuggestion {
  query: string
  displayText: string
  type: 'sender' | 'subject' | 'keyword'
}

export interface SearchHistory {
  id: number
  query: string
  searchType: 'FULLTEXT' | 'ADVANCED'
  resultCount: number
  searchFilters?: string
  executionTimeMs: number
  createdAt: string
}

export interface EmailStatistics {
  totalMessages: number
  unreadMessages: number
  todayMessages: number
  folderStatistics: Record<string, number>
  senderStatistics: Record<string, number>
}

export interface AttachmentSearchResult {
  attachments: EmailAttachment[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export default emailSearchApi