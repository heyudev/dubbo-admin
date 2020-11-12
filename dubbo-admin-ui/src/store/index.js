/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {
    appTitle: 'Dubbo Admin',
    area: null,
    serviceItems: null,
    appItems: null,
    myServiceItems: null,
    myAppItems: null
  },
  mutations: {
    setArea (state, area) {
      state.area = area
    },
    setServiceItems (state, serviceItems) {
      state.serviceItems = serviceItems
    },
    setAppItems (state, appItems) {
      state.appItems = appItems
    },
    setMyServiceItems (state, myServiceItems) {
      state.myServiceItems = myServiceItems
    },
    setMyAppItems (state, myAppItems) {
      state.myAppItems = myAppItems
    }
  },
  actions: {
    changeArea ({commit}, area) {
      commit('setArea', area)
    },
    /**
     * Load service items from server, put results into storage.
     */
    loadServiceItems ({commit}) {
      Vue.prototype.$axios.get('/services')
        .then(response => {
          if (response.status === 200) {
            const serviceItems = response.data
            commit('setServiceItems', serviceItems)
          }
        })
    },
    /**
     * Load application items from server, put results into storage.
     */
    loadAppItems ({commit}) {
      Vue.prototype.$axios.get('/applications')
        .then(response => {
          if (response.status === 200) {
            const appItems = response.data
            commit('setAppItems', appItems)
          }
        })
    },
    /**
     * Load my service items from server, put results into storage.
     */
    loadMyServiceItems ({commit}) {
      Vue.prototype.$axios.get('/myServices')
        .then(response => {
          if (response.status === 200) {
            const myServiceItems = response.data
            commit('setMyServiceItems', myServiceItems)
          }
        })
    },
    /**
     * Load my application items from server, put results into storage.
     */
    loadMyAppItems ({commit}) {
      Vue.prototype.$axios.get('/myApplications')
        .then(response => {
          if (response.status === 200) {
            const myAppItems = response.data
            commit('setMyAppItems', myAppItems)
          }
        })
    }
  },
  getters: {
    /**
     * Get service item arrays with filter
     */
    getServiceItems: (state) => (filter) => {
      return state.serviceItems.filter(e => {
        return (e || '').toLowerCase().indexOf((filter || '').toLowerCase()) > -1
      })
    },
    /**
     * Get application item arrays with filter
     */
    getAppItems: (state) => (filter) => {
      return state.appItems.filter(e => {
        return (e || '').toLowerCase().indexOf((filter || '').toLowerCase()) > -1
      })
    },
    /**
     * Get my service item arrays with filter
     */
    getMyServiceItems: (state) => (filter) => {
      return state.myServiceItems.filter(e => {
        return (e || '').toLowerCase().indexOf((filter || '').toLowerCase()) > -1
      })
    },
    /**
     * Get my application item arrays with filter
     */
    getMyAppItems: (state) => (filter) => {
      return state.myAppItems.filter(e => {
        return (e || '').toLowerCase().indexOf((filter || '').toLowerCase()) > -1
      })
    }
  }
})
