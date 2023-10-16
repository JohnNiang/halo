<template>
  <div>
    <h1>Multi-Factor Authentication</h1>
    <form action="/login/mfa/totp" method="post">
      <div>
        <labe for="name">Enter code</labe>
        <input type="text" name="code" placeholder="Please input TOTP" />
      </div>
      <div>
        <labe for="_csrf">Enter CSRF Token</labe>
        <input type="text" name="_csrf" />
      </div>
      <div>
        <input type="submit" value="Submit" />
      </div>
    </form>
  </div>
</template>

<script lang="ts" setup></script>
