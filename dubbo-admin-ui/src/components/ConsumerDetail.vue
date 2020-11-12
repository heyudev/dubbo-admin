<!--
  - Licensed to the Apache Software Foundation (ASF) under one or more
  - contributor license agreements.  See the NOTICE file distributed with
  - this work for additional information regarding copyright ownership.
  - The ASF licenses this file to You under the Apache License, Version 2.0
  - (the "License"); you may not use this file except in compliance with
  - the License.  You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <v-container grid-list-xl fluid>
    <v-layout row wrap>
      <v-flex sm12>
        <h3>{{$t('detailInfo')}}</h3>
      </v-flex>
      <v-flex lg12>
        <v-data-table
          :items="consumerDetail"
          class="elevation-1"
          hide-actions
          hide-headers>
          <template slot="items" slot-scope="props">
            <td>{{$t(props.item.name)}}</td>
            <td>{{props.item.value}}</td>
          </template>
        </v-data-table>
      </v-flex>
    </v-layout>
  </v-container>
</template>
<script>
  export default {
    data: () => ({
      consumerDetail: []
    }),
    methods: {
      getDetail: function (service, hash) {
        return new Promise((resolve, reject) => {
          this.$axios.get('/service/consumer/' + service + '/' + hash)
            .then(response => {
              resolve(response.data)
            })
        })
      }
    },
    computed: {
      area () {
        return this.$i18n.locale
      }
    },
    mounted: function () {
      let query = this.$route.query
      let meta = {
        'fullUrl': '',
        'appName': '',
        'group': '',
        'service': '',
        'address': '',
        'version': '',
        'side': '',
        'registry': '',
        'methods': '',
        'pid': '',
        'dubbo': '',
        'timestamp': '',
        'check': ''
      }
      let dataId = query['service']
      if (query['registry'] !== '') {
        dataId = dataId + '@' + query['registry']
      }
      let hash = query['hash']
      if (dataId !== '' && hash !== '') {
        this.getDetail(dataId, hash).then(res => {
          let data = []
          Object.keys(meta).forEach(function (key) {
            let item = {}
            item.value = res[key]
            item.name = key
            data.push(item)
          })
          this.consumerDetail = data
        })
      }
    }
  }
</script>

<style scoped>
  .tiny {
    min-width: 30px;
    height: 20px;
    font-size: 8px;
  }
</style>

